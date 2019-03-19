package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification
import shapeless._

class HistogramSpec extends Specification {
  "Histogram No Labels" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        h <- Histogram.noLabelsBuckets[IO](cr, Name("boo"), "Boo ", 0.1, 0.2, 0.3, 0.4)
      } yield h

      test.attempt.unsafeRunSync must beRight
    }
  }

  "Histogram Labelled" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        h <- Histogram.labelledBuckets(cr, Name("boo"), "Boo ", Sized(Label("boo")), {s: String => Sized(s)}, 0.1, 0.2, 0.3, 0.4)
      } yield h

      test.attempt.unsafeRunSync must beRight
    }
  }
}
