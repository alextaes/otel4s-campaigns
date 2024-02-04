package promotions

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import io.circe.syntax._

class CampaignsApi {

  private val users = List("john.doe@acme.come", "john.doe.jr@acme.come", "john.doe.sr@acme.come")

  private val campaigns: Map[Long, Campaign] = (1L to 100L).map { id =>
    id -> Campaign(
       id = id,
       name = s"Stub project #$id",
       description = s"Stub project #$id",
       users = users,
       createdAt = System.currentTimeMillis(),
       createdBy = users.head,
       modifiedAt = System.currentTimeMillis(),
       modifiedBy = users.head
     )
  }.toMap

  val route =
    pathPrefix("campaigns") {
      path(LongNumber) { id =>
        pathEnd {
          get {
            campaigns.get(id).fold(complete(s"Campaign not found with id $id"))(campaign => complete(campaign.asJson))
          }
        }
      }
    }
}
