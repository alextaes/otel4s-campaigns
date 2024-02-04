package promotions
package service

import cats.effect._
import cats.syntax.all._
import fs2.kafka._
import io.circe.generic.auto._
import io.circe.syntax._
import org.typelevel.otel4s.trace.Tracer
import promotions.KafkaConfiguration
import promotions.model.{PromotionEvent, Promotion}
import promotions.telemetry._

class PromotionsKafkaProducer[F[_] : Async : Tracer](kafka: KafkaConfiguration) {

  private val producerSettings: ProducerSettings[F, String, PromotionEvent] = ProducerSettings(
    keySerializer = Serializer[F, String],
    valueSerializer = Serializer[F, String].contramap[PromotionEvent](_.asJson.noSpaces)
  ).withBootstrapServers(kafka.url)

  def sendCreated(ticket: Promotion): F[Unit] = {
    send(PromotionEvent("create", ticket)).void
  }


  def sendUpdated(ticket: Promotion): F[Unit] = {
    send(PromotionEvent("updated", ticket)).void
  }

  private def send(event: PromotionEvent): F[ProducerResult[String, PromotionEvent]] = {
    KafkaProducer.resource(producerSettings)
      .use(_.produceOneTraced(kafka.topic, event.promotion.id.toString, event))
  }
}
