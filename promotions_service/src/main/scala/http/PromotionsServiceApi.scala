package promotions
package http

import cats.effect._
import cats.implicits._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.typelevel.otel4s.trace.Tracer
import promotions.model.{CreatePromotion, UpdatePromotion}
import promotions.service.PromotionsService
import promotions.telemetry._

class PromotionsServiceApi[F[_] : Async : Tracer](service: PromotionsService[F]) {
  private object dsl extends Http4sDsl[F]

  import dsl._

  private implicit val encoder: EntityEncoder[F, Json] = jsonEncoder[F]
  private implicit val createPromotionDecoder: EntityDecoder[F, CreatePromotion] = jsonOf[F, CreatePromotion]
  private implicit val updatePromotionDecoder: EntityDecoder[F, UpdatePromotion] = jsonOf[F, UpdatePromotion]

  private object SearchQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("search")

  private object CampaignIdQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Long]("campaign")

  object CampaignIdPathMatcher {
    def unapply(str: String): Option[Long] = str.toLongOption
  }

  def app: HttpApp[F] = {
    HttpRoutes.of[F] {
        case _@GET -> Root / "promotions" :? SearchQueryParamMatcher(search) +& CampaignIdQueryParamMatcher(campaign) =>
          for {
            promotions <- service.searchPromotions(search, campaign)
            resp <- Ok(promotions.asJson)
          } yield resp

        case req@POST -> Root / "promotions" =>
          for {
            create <- req.as[CreatePromotion]
            promotion <- service.createPromotion(create)
            resp <- Ok(promotion.asJson)
          } yield resp

        case req@PUT -> Root / "promotions" =>
          for {
            update <- req.as[UpdatePromotion]
            promotion <- service.update(update)
            resp <- promotion.map(_.asJson).fold(NotFound(s"promotion with id ${update.id} not found"))(Ok(_))
          } yield resp


        case _@PUT -> Root / "promotions" / CampaignIdPathMatcher(id) =>
          for {
            _ <- service.delete(id)
            resp <- Ok("")
          } yield resp

      }
      .orNotFound
      .traced
  }
}
