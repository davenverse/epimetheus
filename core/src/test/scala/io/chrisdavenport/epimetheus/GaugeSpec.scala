package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification
import shapeless._

class GuageSpec extends Specification {

  "Gauge No Labels" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, "boo", "Boo Gauge")
      } yield gauge

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, "boo", "Boo Gauge")
        _ <- gauge.inc
        out <- gauge.get
      } yield out

      test.unsafeRunSync must_=== 1D
    }

    "Decrease correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, "boo", "Boo Gauge")
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
        gauge <- Gauge.noLabels[IO](cr, "boo", "Boo Gauge")
        _ <- gauge.set(set)
        out <- gauge.get
      } yield out

      test.unsafeRunSync must_=== set
    }
  }

  "Gauge Labelled" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, "boo", "Boo Gauge", Sized("boo"), {s: String => Sized(s)})
      } yield gauge

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, "boo", "Boo Gauge", Sized("boo"), {s: String => Sized(s)})
        _ <- gauge.label("boo").inc
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync must_=== 1D
    }

    "Decrease correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, "boo", "Boo Gauge", Sized("boo"), {s: String => Sized(s)})
        _ <- gauge.label("boo").inc
        _ <- gauge.label("boo").dec
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync must_=== 0D
    }

    "Set correctly" in {
      val set = 52D
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, "boo", "Boo Gauge", Sized("boo"), {s: String => Sized(s)})
        _ <- gauge.label("boo").set(set)
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync must_=== set
    }
  }


}