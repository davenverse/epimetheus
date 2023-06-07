ThisBuild / tlBaseVersion := "0.5" // your current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)

// true by default, set to false to publish to s01.oss.sonatype.org
ThisBuild / tlSonatypeUseLegacyHost := true
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

val Scala213 = "2.13.8"

ThisBuild / crossScalaVersions := Seq("2.12.18", "3.2.2", Scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.last

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


val prometheusV = "0.16.0"
val catsV = "2.9.0"
val catsEffectV = "3.4.8"
val shapelessV = "2.3.9"

val munitCatsEffectV = "1.0.7"


// General Settings
lazy val commonSettings = Seq(

  javacOptions ++= Seq("--release", "8"),

  scalacOptions --= List("-source", "future", "-Xfatal-warnings"),
  Compile / doc / scalacOptions ++=
    Seq("-doc-root-content", (baseDirectory.value.getParentFile / "rootdoc.txt").getAbsolutePath),
  Compile / doc / scalacOptions ++= Opts.doc.title("epimetheus"),

  libraryDependencies ++= Seq(
    "io.prometheus"               % "simpleclient"                % prometheusV,
    "io.prometheus"               % "simpleclient_common"         % prometheusV,
    "io.prometheus"               % "simpleclient_hotspot"        % prometheusV,

    "org.typelevel"               %% "cats-core"                  % catsV,
    "org.typelevel"               %% "cats-effect"                % catsEffectV,

    "org.typelevel"               %%% "munit-cats-effect-3"       % munitCatsEffectV  % Test
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

