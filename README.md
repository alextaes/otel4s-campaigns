# OTEL4s CAMPAIGNS

## POC
POC to test telemetry implementation in Scala.

## Layout

### Subprojects
Main source code divided on following subprojects:
```
/ campaigns_service - stub projects service.
/ load_testing - Gatling tests to simulate user traffic.
/ otel4s - System implementation using mostly "Lightbend" and Typelevel stack: akka, otel4s for instrumentation.
```

### Misc
Apart from source code current repo has the following folders:
```
docker - various docker compose  
```

## How to run
In order to run system

Build necessary docker containers:
```
sbt campaigns_service/docker:publishLocal
sbt promotions_service/docker:publishLocal
```

Run specific system setup using docker compose. For instance :
```
docker-compose -f docker-compose/promotions_service/prometheus-docker-compose.yml up -d
docker-compose -f docker-compose/promotions_service/zipkin-docker-compose.yml up -d
```

After, run load testing to simulate user traffic
```
sbt Gatling/test
```

Check target APM or any monitoring tool and verify telemetry has been sent.

Stop environment using docker compose:
```
docker-compose -f docker-compose/promotions_service/prometheus-docker-compose.yml down
```
