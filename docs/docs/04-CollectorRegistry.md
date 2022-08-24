---
layout: docs
number: 4
title: CollectorRegistry
---

# Collector Registry

A `CollectorRegistry` represents the concurrently shared state which holds the information
of the metrics in question.

You can view this as the entry-point to any metrics. It collects and keeps the relevant data
for all of the metrics.

**Caution:** Only a single metric with a given name can be registered with a `CollectorRegistry`,
attempting to register a metric with the same name twice, will yield an error.

## On Creation

Due to how prometheus scraping occurs, only one  `CollectorRegistry` is generally useful per application. There are generally 2 approaches.

1. Create your own registry. Register Metrics with it. Expose that.
  - Advantages: Full Control Of the Code

2. Use the global `CollectorRegistry.defaultRegistry`
  - Advantages: Easier Interop with Java libraries that may not give an option for interaction with arbitrary CollectorRegistries.

Imports

```scala mdoc:silent
import io.chrisdavenport.epimetheus._
import cats.effect._

import cats.effect.unsafe.implicits.global
```

### Creating Your Own Registry

You can build your own registry. We generally recommend this approach as it keeps
full control of the metrics in your own hands, as well as testability and repeatability.

With this approach you will generally create a registry for your application, and pass it down
to individual components. Which can initialize and register their individual metrics into this
shared space.

```scala mdoc
{
  for {
    cr <- CollectorRegistry.build[IO]
  } yield cr
}
```

You can build your own registry with the default Hotspot Metrics registered with it automatically

```scala mdoc
{
  for {
    cr <- CollectorRegistry.buildWithDefaults[IO]
  } yield cr
}
```

or you can do it yourself.

```scala mdoc
{
  for {
    cr <- CollectorRegistry.build[IO]
    _ <- Collector.Defaults.registerDefaults(cr)
  } yield cr
}
```

### Dealing with the global

Most of the rest of the java community however has used a singleton collector registry. We expose this collector,
as well as a method which initializes the default metrics into the default. This leaves access to the Registry outside of your control, and not necessarily within the call graph of your application.

You have access to the global pool automatically.

```scala mdoc
CollectorRegistry.defaultRegistry[IO]
```

You can also ensure the baseline has been initiated.

```scala mdoc
Collector.Defaults.defaultCollectorRegisterDefaults[IO]
```

## Exporting Your Metrics

Each `CollectorRegistry` exposes a `write004` function which will write out the current value of the metrics with the appropriate encoding for the 004 encoding that Prometheus expects.

```scala mdoc
val exportExample = {
  for {
    cr <- CollectorRegistry.build[IO]
    _ <- Collector.Defaults.registerDefaults(cr)
    currentMetrics <- cr.write004
    _ <- IO(println(currentMetrics))
  } yield ()
}

exportExample.unsafeRunSync()
```

## Conclusion

Regardless which strategy you choose to use to get access to your `CollectorRegistry`, you will need to have one to work and build your metrics moving forward. It is the fundamental shared space on which metrics, and groups of metrics are based upon.
