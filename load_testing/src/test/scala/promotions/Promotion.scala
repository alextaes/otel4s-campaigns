package promotions

case class CreatePromotion(campaign: Long,
                           title: String,
                           description: String,
                           creator: String)

case class UpdatePromotion(id: Long,
                           campaign: Long,
                           title: String,
                           description: String,
                           updater: String)
