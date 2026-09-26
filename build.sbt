import com.typesafe.tools.mima.core._

ThisBuild / tlBaseVersion := "0.7" // your current series x.y

// v0.6.1 was tagged, but the release failed in CI against the sunset OSSRH
// endpoint, so the artifact never reached Maven Central. Drop it from the MiMa
// baseline, which would otherwise fail to resolve it.
ThisBuild / tlMimaPreviousVersions ~= (_ - "0.6.1")

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / githubWorkflowSbtCommand := "./sbt"

val Scala213 = "2.13.18"

ThisBuild / crossScalaVersions := Seq("3.3.8", Scala213)
ThisBuild / scalaVersion := Scala213

lazy val `epimetheus` = tlCrossRootProject
  .settings(commonSettings)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "epimetheus"
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core)
  .settings(
    laikaTheme := tlSiteHelium.value.site
      .topNavigationBar(
        homeLink = laika.helium.config.IconLink.internal(laika.ast.Path.Root / "index.md", laika.helium.config.HeliumIcon.home)
      )
      .build
  )

val prometheusV = "1.4.3"
val catsV = "2.13.0"
val catsEffectV = "3.7.1"
val shapelessV = "2.3.9"

val munitCatsEffectV = "2.2.1"


// General Settings
lazy val commonSettings = Seq(

  mimaBinaryIssueFilters := List({(_: Problem) => false}), // TODO: remove this once switched to next major version

  javacOptions ++= Seq("--release", "8"),
  // javac's --release only governs the Java sources. Without the scalac
  // counterpart the Scala code can link against JDK APIs newer than 8 and
  // still produce Java 8 bytecode, which then fails at runtime on a Java 8
  // consumer. That is how write004 shipped calling a Java 10 method.
  scalacOptions ++= Seq("-release", "8"),

  scalacOptions --= List("-source", "future", "-Xfatal-warnings"),
  Compile / doc / scalacOptions ++=
    Seq("-doc-root-content", (baseDirectory.value.getParentFile / "rootdoc.txt").getAbsolutePath),
  Compile / doc / scalacOptions ++= Opts.doc.title("epimetheus"),

  libraryDependencies ++= Seq(
    "io.prometheus"               % "prometheus-metrics-core"                % prometheusV,
    "io.prometheus"               % "prometheus-metrics-instrumentation-jvm" % prometheusV,
    "io.prometheus"               % "prometheus-metrics-exposition-formats"  % prometheusV,

    "org.typelevel"               %% "cats-core"                             % catsV,
    "org.typelevel"               %% "cats-effect"                           % catsEffectV,

    "org.typelevel"               %%% "munit-cats-effect"                  % munitCatsEffectV  % Test,
    "org.scala-lang.modules"      %%% "scala-collection-compat"              % "2.11.0"          % Test
  ),
  libraryDependencies ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) { case Some((2, _)) =>
      Seq(
        "org.scala-lang"              % "scala-reflect"               % scalaVersion.value,
        "com.chuusai"                 %% "shapeless"                  % shapelessV
      )
    }
    .toList
    .flatten
)

