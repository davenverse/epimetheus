---
layout: docs
number: 1
title: Introduction
---

# {{page.title}}

This is a short book about **epimetheus** a pure functional Prometheus integration for Scala.

## Target Audience

This library is designed for people who are interested in typed, pure functional programming. If you are not a [Cats](https://github.com/typelevel/cats) or [Cats-Effect](https://github.com/typelevel/cats-effect) user or are not familiar with functional I/O and monadic effects, you may need to go slowly and may want to spend some time reading [Functional Programming in Scala](http://manning.com/bjarnason/), which introduces all of the ideas that you will find when exploring **epimetheus**.

Having said this, if you find yourself confused or frustrated by this documentation or the **epimetheus** API, *please* ask a question on [Gitter](https://gitter.im/ChristopherDavenport/epimetheus), file an [issue](https://github.com/ChristopherDavenport/epimetheus/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc) and ask for help. Both the library and the documentation are young and are changing quickly, and it is inevitable that some things will be unclear. Accordingly, **this book is updated continuously** to address problems and omissions.

## Scala Setup

On the Scala side you just need a project with the proper dependencies. A minimal `build.sbt` would look something like this.

The most recent stable version - [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/epimetheus_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/epimetheus_2.12)

```scala
scalaVersion := "{{site.scalaVersion}}" // Scala {{site.scalaVersions}}

scalacOptions += "-Ypartial-unification" // 2.11.9+

lazy val epimetheusV = "{{site.epimetheusVersion}}"

libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "epimetheus"     % epimetheusV
)
```

The `-Ypartial-unification` compiler flag enables a bug fix that makes working with functional code significantly easier. See the Cats [Getting Started](https://github.com/typelevel/cats#getting-started) for more info on this if it interests you.

## Conventions

Each page begins with some imports, like this.

```scala mdoc:silent
import cats._, cats.data._, cats.implicits._
import cats.effect._, cats.effect.implicits._
import shapeless._
import io.chrisdavenport.epimetheus._
```

After that there is text interspersed with code examples. Sometimes definitions will stand alone.

```scala mdoc:silent
case class Person(name: String, age: Int)
val nel = NonEmptyList.of(Person("Bob", 12), Person("Alice", 14))
```

And sometimes they will appear as a REPL interaction.

```scala mdoc
nel.head
nel.tail
```

Sometimes we demonstrate that something doesn't compile. In such cases it will be clear from the context that this is expected, and not a problem with the documentation.

```scala mdoc:nofail
woozle(nel) // doesn't compile
```

Many thanks to to the [Book of Doobie](https://github.com/tpolecat/doobie) for clearly outlining this.

## Feedback and Contributions

Feedback on **epimetheus** or this book is genuinely welcome. Please feel free to file a [pull request](hhttps://github.com/ChristopherDavenport/epimetheus) if you have a contribution, or file an [issue](https://github.com/ChristopherDavenport/epimetheus/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc), or chat with us on [Gitter](https://gitter.im/ChristopherDavenport/epimetheus).