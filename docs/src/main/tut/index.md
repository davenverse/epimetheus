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

```tut:silent
import io.chrisdavenport.epimetheus._
import cats.effect._
```

### Counter Example

```tut:book
val noLabelsCounterExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    counter <- Counter.noLabels(cr, Name("counter_total"), "Example Counter")
    _ <- counter.inc
    currentMetrics <- cr.write004
  } yield currentMetrics
}

noLabelsCounterExample.unsafeRunSync
```

### Gauge Example

```tut:book
val noLabelsGaugeExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    gauge <- Gauge.noLabels(cr, Name("gauge_total"), "Example Gauge")
    _ <- gauge.inc
    _ <- gauge.inc
    _ <- gauge.dec
    currentMetrics <- cr.write004
  } yield currentMetrics
}

noLabelsGaugeExample.unsafeRunSync
```

### Histogram Example

```tut:book
val noLabelsHistogramExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    h <- Histogram.noLabels(cr, Name("example_histogram"), "Example Histogram")
    _ <- h.observe(0.2)
    // Not for 0.1 _ <- h.timed(T.sleep(1.second), SECONDS)
    currentMetrics <- cr.write004
  } yield currentMetrics
}

noLabelsHistogramExample.unsafeRunSync
```

### Summary Example

```tut:book
val noLabelsSummaryExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    s <- Summary.noLabels(cr, Name("example_summary"), "Example Summary", Summary.quantile(0.5,0.05))
    _ <- s.observe(0.1)
    _ <- s.observe(0.2)
    _ <- s.observe(1.0)
    currentMetrics <- cr.write004
  } yield currentMetrics
}

noLabelsSummaryExample.unsafeRunSync
```