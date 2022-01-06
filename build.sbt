import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val Scala213 = "2.13.7"

ThisBuild / crossScalaVersions := Seq("2.12.15", "3.1.0", Scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.last

lazy val `epimetheus` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "epimetheus"
  )

lazy val site = project.in(file("site"))
  .disablePlugins(MimaPlugin)
  .enablePlugins(DavenverseMicrositePlugin)
  .settings(commonSettings)
  .dependsOn(core)


val prometheusV = "0.11.0"
val catsV = "2.7.0"
val catsEffectV = "3.3.3"
val shapelessV = "2.3.7"

val munitCatsEffectV = "1.0.5"


// General Settings
lazy val commonSettings = Seq(

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

