import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "3.3.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.37.0-play-27",
    "uk.gov.hmrc" %% "play-frontend-govuk" % "0.60.0-play-27",
    "uk.gov.hmrc" %% "play-language" % "4.7.0-play-27",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.11.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.11.1",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.1",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.11.1"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-27" % "3.3.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    "org.jsoup" % "jsoup" % "1.13.1" % Test,
    "com.typesafe.play" %% "play-test" % current % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test, it",
    "org.mockito" %% "mockito-scala-scalatest" % "1.14.4" % "test, it",
    "com.github.tomakehurst" % "wiremock" % "2.25.1" % "test, it"
  )
}
