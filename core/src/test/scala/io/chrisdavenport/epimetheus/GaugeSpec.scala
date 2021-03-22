package io.chrisdavenport.epimetheus

import cats.effect._
import cats.effect.unsafe.implicits.global
import org.specs2.mutable.Specification
import shapeless._
import io.chrisdavenport.epimetheus.implicits._
import scala.concurrent.duration._

class GuageSpec extends Specification {

  "Gauge No Labels" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
      } yield gauge

      test.attempt.unsafeRunSync() must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        _ <- gauge.inc
        out <- gauge.get
      } yield out

      test.unsafeRunSync() must_=== 1D
    }

    "Decrease correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        _ <- gauge.inc
        _ <- gauge.dec
        out <- gauge.get
      } yield out

      test.unsafeRunSync() must_=== 0D
    }

    "Set correctly" in {
      val set = 52D
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        _ <- gauge.set(set)
        out <- gauge.get
      } yield out

      test.unsafeRunSync() must_=== set
    }
  }

  "Gauge Labelled" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, Name("boo"), "Boo Gauge", Sized(Label("boo")), {s: String => Sized(s)})
      } yield gauge

      test.attempt.unsafeRunSync() must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, Name("boo"), "Boo Gauge", Sized(Label("boo")), {s: String => Sized(s)})
        _ <- gauge.label("boo").inc
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync() must_=== 1D
    }

    "Decrease correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, Name("boo"), "Boo Gauge", Sized(Label("boo")), {s: String => Sized(s)})
        _ <- gauge.label("boo").inc
        _ <- gauge.label("boo").dec
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync() must_=== 0D
    }

    "Set correctly" in {
      val set = 52D
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.labelled(cr, Name("boo"), "Boo Gauge", Sized(Label("boo")), {s: String => Sized(s)})
        _ <- gauge.label("boo").set(set)
        out <- gauge.label("boo").get
      } yield out

      test.unsafeRunSync() must_=== set
    }
  }

  "Gauge Convenience" should {
    "incIn an operation succesfully" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        defer <- Deferred[IO, Unit]
        fib <- gauge.incIn(defer.get).start
        _ <- IO.sleep(1.second)
        current <- gauge.get
        _ <- defer.complete(())
        _ <- fib.join
        after <- gauge.get
      } yield (current, after)

      test.unsafeRunSync() must_=== ((1, 0))
    }

    "incByIn an operation succesfully" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        defer <- Deferred[IO, Unit]
        fib <- gauge.incByIn(defer.get, 10).start
        _ <- IO.sleep(1.second)
        current <- gauge.get
        _ <- defer.complete(())
        _ <- fib.join
        after <- gauge.get
      } yield (current, after)

      test.unsafeRunSync() must_=== ((10, 0))
    }

    "decIn an operation succesfully" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        _ <- gauge.inc
        defer <- Deferred[IO, Unit]
        fib <- gauge.decIn(defer.get).start
        _ <- IO.sleep(1.second)
        current <- gauge.get
        _ <- defer.complete(())
        _ <- fib.join
        after <- gauge.get
      } yield (current, after)

      test.unsafeRunSync() must_=== ((0, 1))
    }

    "decByIn an operation succesfully" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        gauge <- Gauge.noLabels[IO](cr, Name("boo"), "Boo Gauge")
        _ <- gauge.incBy(10)
        defer <- Deferred[IO, Unit]
        fib <- gauge.decByIn(defer.get, 10).start
        _ <- IO.sleep(1.second)
        current <- gauge.get
        _ <- defer.complete(())
        _ <- fib.join
        after <- gauge.get
      } yield (current, after)

      test.unsafeRunSync() must_=== ((0, 10))
    }
  }


}
