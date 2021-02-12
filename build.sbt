import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import bloop.integrations.sbt.BloopDefaults

val appName = "integration-catalogue-admin-frontend"

val silencerVersion = "1.7.0"

val jettyVersion = "9.2.24.v20180105"

val jettyOverrides = Seq(
  "org.eclipse.jetty" % "jetty-server" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-security" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-servlets" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-xml" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-client" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-http" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-io" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty" % "jetty-util" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-api" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-common" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-client" % jettyVersion % IntegrationTest
)


lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.12",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    Test / unmanagedSourceDirectories += baseDirectory(_ / "test-common").value,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
    ),
    // ***************
    // Use the silencer plugin to suppress warnings
    // You may turn it on for `views` too to suppress warnings from unused imports in compiled twirl templates, but this will hide other warnings.
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    dependencyOverrides ++= jettyOverrides
    // ***************
  )
  .settings(publishingSettings,
    scoverageSettings)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings))
  .settings(resolvers += Resolver.jcenterRepo) 
  .settings(unmanagedResourceDirectories in IntegrationTest += (baseDirectory in IntegrationTest).value / "it" / "resources")
  .settings(unmanagedSourceDirectories in IntegrationTest += (baseDirectory in IntegrationTest).value / "test-common")
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := ";.*\\.domain\\.models\\..*;uk\\.gov\\.hmrc\\.BuildInfo;.*\\.Routes;.*\\.RoutesPrefix;;Module;GraphiteStartUp;.*\\.Reverse[^.]*",
    ScoverageKeys.coverageMinimum := 86,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}