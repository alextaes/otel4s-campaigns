import com.typesafe.sbt.packager.docker.DockerChmodType

name := "otel4s-campaigns"
version := "0.1"


lazy val load_testing =
  (project in file("load_testing")).
    enablePlugins(GatlingPlugin).
    settings(
      scalaVersion := "2.13.10",
      libraryDependencies := Dependencies.loadTesting
    )

lazy val campaigns_service =
  (project in file("campaigns_service")).
    enablePlugins(JavaAppPackaging, DockerPlugin).
    settings(
      scalaVersion := "2.13.10",

      resolvers += "confluent" at "https://packages.confluent.io/maven/",
      libraryDependencies := Dependencies.lightbend,

      dockerExposedPorts := Seq(10000),
      dockerBaseImage := "openjdk:17",
      Docker / packageName := "campaigns_service",
      Docker / version := "latest"
    )

lazy val promotions_service =
  (project in file("promotions_service")).
    enablePlugins(JavaAgent, JavaAppPackaging, DockerPlugin).
    settings(
      scalaVersion := "2.13.10",

      resolvers += "confluent" at "https://packages.confluent.io/maven/",
      libraryDependencies := Dependencies.trace4cats,

      javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.24.0",
      javaOptions += "-Dotel.java.global-autoconfigure.enabled=true",
      javaOptions += "-Dotel.javaagent.debug=true",

      mainClass := Some("promotions.PromotionsServiceApplication"),

      dockerExposedPorts := Seq(10000),
      dockerBaseImage := "openjdk:17",
      Docker / packageName := "promotions_service",
      Docker / version := "latest",
      Docker / dockerChmodType := DockerChmodType.UserGroupWriteExecute
    )

