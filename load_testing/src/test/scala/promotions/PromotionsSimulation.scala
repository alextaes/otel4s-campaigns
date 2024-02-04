package promotions

import io.gatling.core.Predef._
import io.gatling.core.body.StringBody
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation

import io.circe.generic.auto._
import io.circe.syntax._

import java.nio.charset.Charset
import scala.concurrent.duration._
import scala.language.postfixOps


class PromotionsSimulation extends Simulation {

  val httpProtocol = http.baseUrl("http://localhost:10001").contentTypeHeader("application/json")
  val users = 10
  val promotions = 1 to 20
  val campaigns = 1 to 10

  def execCreatePromotion(id: Int, campaignId: Long) = {
    val body = CreatePromotion(
      campaign = campaignId,
      title = s"Test promotion title $id",
      description = s"Test promotion description: $id.",
      creator = "john.doe@acme.com"
    )

    exec {
      http(s"Create campaign promotion: $campaignId")
        .post("/promotions")
        .body(StringBody(body.asJson.noSpaces, Charset.forName("UTF-8")))
        .header("Content-Type", "application/json")
        .check(status is 200)
        .requestTimeout(1 minute)
    }
  }

  def execCreatePromotions = {
    for {
      ticketId <- promotions
      campaignId <- campaigns
    } yield execCreatePromotion(ticketId, campaignId)
  }


  def execSearchCampaignPromotions(campaign: Long) = {
    exec {
      http(s"Search campaign promotions: $campaign")
        .get(s"/promotions?search=test%20promotion&campaign=$campaign")
        .check(status is 200)
        .requestTimeout(1 minute)
    }
  }

  def execSearchPromotions = {
    for {
      campaignId <- campaigns
    } yield execSearchCampaignPromotions(campaignId)
  }

  def execUpdatePromotion(id: Int, campaignId: Long) = {
    val body = UpdatePromotion(
      id = id,
      campaign = campaignId,
      title = s"Updated test promotion title $id",
      description = s"Updated test promotion description: $id.",
      updater = "john.doe@acme.com"
    )

    exec {
      http(s"Update promotion: $campaignId")
        .put("/promotions")
        .body(StringBody(body.asJson.noSpaces, Charset.forName("UTF-8")))
        .header("Content-Type", "application/json")
        .check(status is 200)
    }
  }

  def execUpdatePromotions = {
    for {
      promotionId <- promotions
      campaignId <- campaigns
    } yield execUpdatePromotion(promotionId, campaignId)
  }

  def execDeletePromotion(id: Int) = {
    exec {
      http(s"Delete $id")
        .delete(s"/promotions/$id")
    }
  }

  def execDeletePromotions = {
    for {
      promotionId <- 1 to 30
    } yield execDeletePromotion(promotionId)
  }

  setUp(
    scenario("Simulate users traffic").
      exec(execCreatePromotions).
      exec(execCreatePromotions).
      exec(execCreatePromotions).
      exec(execCreatePromotions).
      inject(atOnceUsers(users)).
      protocols(httpProtocol)
  )
}

