package promotions
package model

case class Campaign(id: Long,
                    name: String,
                    description: String,
                    users: List[String],
                    createdAt: Long,
                    createdBy: String,
                    modifiedAt: Long,
                    modifiedBy: String)
