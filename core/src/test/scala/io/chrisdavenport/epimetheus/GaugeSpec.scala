package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification

class GuageSpec extends Specification {

  "Gauge" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.build[IO](cr, "boo", "Boo Gauge")
      } yield gauge

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.build[IO](cr, "boo", "Boo Gauge")
        _ <- gauge.inc
        out <- gauge.get
      } yield out

      test.unsafeRunSync must_=== 1D
    }

    "Decrease correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.build[IO](cr, "boo", "Boo Gauge")
        _ <- gauge.inc
        _ <- gauge.dec
        out <- gauge.get
      } yield out

      test.unsafeRunSync must_=== 0D
    }

    "Set correctly" in {
      val set = 52D
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.build[IO](cr, "boo", "Boo Gauge")
        _ <- gauge.set(set)
        out <- gauge.get
      } yield out

      test.unsafeRunSync must_=== set
    }


  }

}