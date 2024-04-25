---
layout: home

---

# epimetheus - An Afterthought of Prometheus [![Build Status](https://travis-ci.com/ChristopherDavenport/epimetheus.svg?branch=master)](https://travis-ci.com/ChristopherDavenport/epimetheus) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/epimetheus_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/epimetheus_2.12)

## Quick Start

To use epimetheus in an existing SBT project with Scala 2.11 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "epimetheus" % "<version>"
)
```

## Quick Examples

First Imports.

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import io.chrisdavenport.epimetheus.implicits._
import cats.effect._

import scala.concurrent.duration._

import cats.effect.unsafe.implicits.global
```

### Counter Example

```scala mdoc
val noLabelsCounterExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    counter <- Counter.noLabels(pr, Name("counter"), "Example Counter")
    _ <- counter.inc
    currentMetrics <- pr.write004
  } yield currentMetrics
}

noLabelsCounterExample.unsafeRunSync()
```

### Gauge Example

```scala mdoc
val noLabelsGaugeExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    gauge <- Gauge.noLabels(pr, Name("gaugetotal"), "Example Gauge")
    _ <- gauge.inc
    _ <- gauge.inc
    _ <- gauge.dec
    currentMetrics <- pr.write004
  } yield currentMetrics
}

noLabelsGaugeExample.unsafeRunSync()
```

### Histogram Example

```scala mdoc
val noLabelsHistogramExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    h <- Histogram.noLabels(pr, Name("example_histogram"), "Example Histogram")
    _ <- h.observe(0.2)
    _ <- h.timed(Temporal[IO].sleep(1.second), SECONDS)
    currentMetrics <- pr.write004
  } yield currentMetrics
}

noLabelsHistogramExample.unsafeRunSync()
```

### Summary Example

```scala mdoc
val noLabelsSummaryExample = {
  for {
    pr <- PrometheusRegistry.build[IO]
    s <- Summary.noLabels(pr, Name("example_summary"), "Example Summary", Summary.quantile(0.5,0.05))
    _ <- s.observe(0.1)
    _ <- s.observe(0.2)
    _ <- s.observe(1.0)
    currentMetrics <- pr.write004
  } yield currentMetrics
}

noLabelsSummaryExample.unsafeRunSync()
```
