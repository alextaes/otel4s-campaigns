package promotions
package model

case class Promotion(id: Long,
                     campaign: Long,
                     title: String,
                     description: String,
                     createdAt: Long,
                     createdBy: String,
                     modifiedAt: Long,
                     modifiedBy: String) {
  def toSearch: SearchPromotion = {
    SearchPromotion(id, campaign, title, description)
  }
}

case class PromotionEvent(event: String, promotion: Promotion)


case class SearchPromotion(id: Long,
                           campaign: Long,
                           title: String,
                           description: String)


case class CreatePromotion(campaign: Long,
                           title: String,
                           description: String,
                           creator: String)

case class UpdatePromotion(id: Long,
                           campaign: Long,
                           title: String,
                           description: String,
                           updater: String)

