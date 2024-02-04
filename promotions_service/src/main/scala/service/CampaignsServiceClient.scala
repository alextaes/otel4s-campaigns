package promotions
package service

import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.CirceEntityCodec._
import org.typelevel.otel4s.trace.Tracer
import promotions.CampaignsServiceConfiguration
import promotions.model.Campaign
import promotions.telemetry._

class CampaignsServiceClient[F[_] : Async : Sync : Tracer](configuration: CampaignsServiceConfiguration) {

  def findCampaign(campaignId: Long): F[Option[Campaign]] = {
    BlazeClientBuilder[F].resource.use { client =>
      val request = Request[F](Method.GET, Uri.unsafeFromString(s"${configuration.url}/campaigns/$campaignId"))
      client.runTraced(request).flatMap { response =>
        response.status match {
          case Status.NotFound => Sync[F].pure(none)
          case Status.Ok => response.as[Campaign].map(_.some)
          case status =>
            response.as[String].flatMap { body =>
              Sync[F].raiseError(new Exception(s"Failure response code received $status: $body"))
            }
        }
      }
    }
  }
}
