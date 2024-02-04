package promotions
package service

import cats.effect.{Async, Sync}
import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.fields.TextField
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexResponse
import com.sksamuel.elastic4s.requests.searches.queries.Query
import io.circe.generic.auto._
import org.typelevel.otel4s.trace.Tracer
import promotions.ElasticsearchConfiguration
import promotions.model.{SearchPromotion, Promotion}
import promotions.telemetry._

import scala.concurrent.ExecutionContext


class PromotionsElasticRepository[F[_] : Async : Sync : Tracer](configuration: ElasticsearchConfiguration)
                                                               (implicit ec: ExecutionContext) {

  private val client: ElasticClient = {
    ElasticClient(JavaClient(ElasticProperties(configuration.url)))
  }

  def init: F[Response[CreateIndexResponse]] = {
    client.executeTraced(
      createIndex("promotions").mapping(properties(TextField("title"), TextField("description")))
    )
  }

  def indexPromotion(promotion: Promotion): F[Unit] = {
    client.executeTraced {
      indexInto("promotions").
        id(promotion.id.toString).
        source(promotion.toSearch).
        refresh(RefreshPolicy.Immediate)
    }.as(())
  }

  def searchPromotions(searchQuery: Option[String], campaignId: Option[Long]): F[List[SearchPromotion]] = {
    val queries: List[Query] = searchQuery.toList.map(query) ++ campaignId.toList.map(termQuery("campaign", _))
    client.executeTraced(search("promotions").bool(must(queries))).map(_.result.to[SearchPromotion].toList)
  }

  def deletePromotion(id: Long): F[Unit] = {
    client.executeTraced(deleteById("promotions", id.toString).refresh(RefreshPolicy.Immediate)).as(())
  }
}
