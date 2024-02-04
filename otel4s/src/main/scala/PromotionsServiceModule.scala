package promotions

import cats.implicits._
import cats.effect.{Async, Concurrent, LiftIO, Resource, Sync}
import io.opentelemetry.api.GlobalOpenTelemetry
import org.typelevel.otel4s.Otel4s
import org.typelevel.otel4s.java.OtelJava
import org.typelevel.otel4s.metrics.{Meter, UpDownCounter}
import org.typelevel.otel4s.trace.Tracer
import promotions.http.PromotionsServiceApi
import promotions.service._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class PromotionsServiceModule[F[_] : Sync : Async : Concurrent : Tracer](
                                                                          configuration: PromotionsConfiguration,
                                                                          promotionsCounter: UpDownCounter[F, Long]) {
  implicit lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  lazy val kafka = new PromotionsKafkaProducer[F](configuration.kafka)
  lazy val repo = new PromotionsPostgresRepository[F](configuration.postgre)
  lazy val elastic = new PromotionsElasticRepository[F](configuration.elasticsearch)
  lazy val campaigns = new CampaignsServiceClient[F](configuration.campaigns)
  lazy val service = new PromotionsService[F](repo, elastic, kafka, campaigns, promotionsCounter)
  lazy val api = new PromotionsServiceApi[F](service)
}

object PromotionsServiceModule {

  def build[F[_] : Sync : Async : Concurrent : LiftIO] = {
    otelResource.use(buildInternal[F])
  }

  private def otelResource[F[_] : Sync : Async : LiftIO]: Resource[F, Otel4s[F]] = {
    Resource
      .eval(Sync[F].delay(GlobalOpenTelemetry.get))
      .evalMap(OtelJava.forAsync[F])
  }

  private def buildInternal[F[_] : Sync : Async : Concurrent : LiftIO](otel: Otel4s[F]) = {
    for {
      configuration <- Sync[F].delay(PromotionsConfiguration.load)

      traceProvider <- otel.tracerProvider.get("promotions-service")
      metricsProvider <- otel.meterProvider.get("promotions-service")

      promotionsCounter <- metricsProvider
        .upDownCounter("promotions_count")
        .withUnit("1")
        .withDescription("Promotions count")
        .create

    } yield {
      implicit val tracer: Tracer[F] = traceProvider
      new PromotionsServiceModule[F](configuration, promotionsCounter)
    }
  }
}
