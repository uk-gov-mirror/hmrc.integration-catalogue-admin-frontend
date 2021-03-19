import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.6.2"

  val compile = Seq(
    "uk.gov.hmrc"                     %% "bootstrap-frontend-play-27" % "3.4.0",
    "uk.gov.hmrc"                     %% "play-frontend-hmrc"         % "0.51.0-play-27",
    "uk.gov.hmrc"                     %% "play-frontend-govuk"        % "0.65.0-play-27",
    "com.beachape"                    %% "enumeratum-play-json"       % enumeratumVersion,
    "com.fasterxml.jackson.core"       % "jackson-core"               % "2.12.2",
    "com.fasterxml.jackson.core"       % "jackson-annotations"        % "2.12.2",
    "com.fasterxml.jackson.core"       % "jackson-databind"           % "2.12.2",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml"    % "2.12.2",
    "com.typesafe.play"               %% "play-json-joda"             % "2.9.2",
    "org.typelevel"                   %% "cats-core"                  % "2.4.2"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-27"   % "3.3.0"  % Test,
    "org.pegdown"             % "pegdown"                  % "1.6.0"  % "test, it",
    "org.jsoup"               % "jsoup"                    % "1.13.1" % Test,
    "com.typesafe.play"      %% "play-test"                % current  % Test,
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.36.8" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "4.0.3"  % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.14.4" % "test, it",
    "com.github.tomakehurst"  % "wiremock"                 % "2.25.1" % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone" % "2.27.1" % "test, it"
  )
}
