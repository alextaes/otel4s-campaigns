package promotions

import com.comcast.ip4s.{Host, Port}
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.UserValidationFailed

case class ElasticsearchConfiguration(url: String)

case class PostgreConfiguration(url: String)

case class KafkaConfiguration(url: String, topic: String)

case class ApplicationConfiguration(host: String, port: Int)

case class CampaignsServiceConfiguration(url: String)

case class PromotionsConfiguration(elasticsearch: ElasticsearchConfiguration,
                                   postgre: PostgreConfiguration,
                                   kafka: KafkaConfiguration,
                                   campaigns: CampaignsServiceConfiguration,
                                   application: ApplicationConfiguration)

object PromotionsConfiguration {
  def load: PromotionsConfiguration = ConfigSource.default.loadOrThrow[PromotionsConfiguration]
}
