package promotions

import io.circe.Codec
import io.circe.generic.semiauto._

case class Campaign(id: Long,
                   name: String,
                   description: String,
                   users: List[String],
                   createdAt: Long,
                   createdBy: String,
                   modifiedAt: Long,
                   modifiedBy: String)

object Campaign {
  implicit val codec: Codec[Campaign] = deriveCodec
}