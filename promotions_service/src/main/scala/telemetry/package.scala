package promotions

package object telemetry
  extends ClientMiddleware
    with ServerMiddleware
    with ElasticMiddleware
    with KafkaProducerMiddleware


