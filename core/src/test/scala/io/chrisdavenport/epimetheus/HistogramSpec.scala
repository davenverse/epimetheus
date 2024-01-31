package io.chrisdavenport.epimetheus

import cats.effect._

class HistogramSpec extends munit.CatsEffectSuite {
  test("Histogram No Labels: Register cleanly in the collector") {
    val test = for {
      pr <- PrometheusRegistry.build[IO]
      h <- Histogram.noLabelsBuckets[IO](pr, Name("boo"), "Boo ", 0.1, 0.2, 0.3, 0.4)
    } yield h

    test.attempt.map(_.isRight).assert
  }

  test("Histogram Labelled: Register cleanly in the collector") {
    val test = for {
      pr <- PrometheusRegistry.build[IO]
      h <- Histogram.labelledBuckets(pr, Name("boo"), "Boo ", Sized(Label("boo")), { (s: String) => Sized(s) }, 0.1, 0.2, 0.3, 0.4)
    } yield h

    test.attempt.map(_.isRight).assert
  }

  test("Histogram: observe with exemplars (map)") {
    val setup = for {
      cr <- PrometheusRegistry.build[IO]
      h <- Histogram.noLabels(cr, Name("boo"), "Boo ")
    } yield h

    val test = for {
      h <- setup
      _ <- h.observeWithExemplar(0.1, Map("exemplar1" -> "value1"))
    } yield ()

    test.attempt.map(_.isRight).assert

  }

  test("Histogram: observe with exemplars (tuples)") {
    val setup = for {
      cr <- PrometheusRegistry.build[IO]
      h <- Histogram.noLabels(cr, Name("boo"), "Boo ")
    } yield h

    val test = for {
      h <- setup
      _ <- h.observeWithExemplar(0.1, "exemplar1" -> "value1")
    } yield ()

    test.attempt.map(_.isRight).assert

  }

}
