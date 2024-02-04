package promotions

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

object CampaignsServiceApplication extends LazyLogging {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "application-system")

    val campaignsApi = new CampaignsApi()

    val binding = Http()
      .newServerAt("0.0.0.0", 10000)
      .bind(campaignsApi.route)


    val shutdown = CoordinatedShutdown(system)
    shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "http-unbind") { () =>
      binding.flatMap(_.unbind()).map(_ => Done)
    }

    shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-unbind") { () =>
      binding.flatMap(_.terminate(10.seconds)).map(_ => Done)
    }
  }
}
