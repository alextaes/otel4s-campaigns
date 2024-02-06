package promotions
package service

import cats.data.NonEmptyList
import cats.implicits._
import cats.effect.{Async, Sync}
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.{GlobalOpenTelemetry, OpenTelemetry}
import io.opentelemetry.context.ContextStorage
import org.http4s.dsl.Http4sDslBinCompat
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.java.metrics.Metrics
import org.typelevel.otel4s.metrics.{Meter, UpDownCounter}
import promotions.model.{CreatePromotion, Promotion, UpdatePromotion}

class PromotionsService[F[_]](postgres: PromotionsPostgresRepository[F],
                              elastic: PromotionsElasticRepository[F],
                              campaigns: CampaignsServiceClient[F],
                              promotionsCounter: UpDownCounter[F, Long])
                             (implicit F: Sync[F], A: Async[F]) {

  implicit def logger: Logger[F] = Slf4jLogger.getLogger[F]

  def createPromotion(create: CreatePromotion): F[Promotion] = {
    val promotion = Promotion(
      id = -1,
      campaign = create.campaign,
      title = create.title,
      description = create.description,
      createdAt = System.currentTimeMillis(),
      createdBy = create.creator,
      modifiedAt = System.currentTimeMillis(),
      modifiedBy = create.creator,
    )

    for {
      _ <- logger.info(s"Creating promotion: $promotion")

      _ <- logger.info(s"Checking campaigns exists: ${promotion.campaign}")
      _ <- verifyCampaign(promotion.campaign)

      _ <- logger.info(s"Persisting promotion in database: $promotion")
      promotion <- postgres.create(promotion)

      _ <- logger.info(s"Indexing promotion in Elasticsearch: $promotion")
      _ <- elastic.indexPromotion(promotion)

      _ <- promotionsCounter.add(1, Attribute("campaign.id", promotion.id)) // Increase metrics counter
      _ <- logger.info(s"Created promotion: $promotion")
    } yield promotion
  }

  def searchPromotions(query: Option[String], campaign: Option[Long]): F[List[Promotion]] = {
    for {
      foundPromotions <- elastic.searchPromotions(query, campaign)
      _ <- logger.info(s"Found promotions: $foundPromotions")
      promotions <- NonEmptyList.fromList(foundPromotions).fold(F.pure(List.empty[Promotion])) { promotions =>
        postgres.getByIds(promotions.map(_.id))
      }
    } yield promotions
  }

  def update(update: UpdatePromotion): F[Option[Promotion]] = {
    for {
      _ <- logger.info(s"Updating promotion: $update")
      _ <- verifyCampaign(update.campaign)
      existing <- postgres.getById(update.id)
      updated = existing.map(
        _.copy(
          campaign = update.campaign,
          title = update.title,
          description = update.description,
          modifiedBy = update.updater,
          modifiedAt = System.currentTimeMillis()
        )
      )
      _ <- updated.fold(F.unit) { promotion =>
        for {
          _ <- postgres.update(promotion)
          _ <- elastic.indexPromotion(promotion)
        } yield ()
      }
      _ <- logger.info(s"Updated promotion: $updated")
    } yield
      updated
  }

  def delete(id: Long): F[Boolean] = {
    for {
      count <- postgres.delete(id)
      _ <- elastic.deletePromotion(id)
      _ <- promotionsCounter.add(-1)
    } yield count > 0
  }

  private def verifyCampaign(campaignId: Long): F[Unit] = {
    for {
      campaign <- campaigns.findCampaign(campaignId)
      _ <- campaign.fold(F.raiseError[Unit](new Exception(s"No campaign found: $campaign")))(_ => F.unit)
    } yield ()
  }
}
