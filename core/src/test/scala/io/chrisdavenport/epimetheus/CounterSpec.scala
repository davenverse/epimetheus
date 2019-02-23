package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification
import shapeless._

class CounterSpec extends Specification {

  "Counter No Labels" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.noLabels[IO](cr, "boo", "Boo Counter")
      } yield counter

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.noLabels[IO](cr, "boo", "Boo Counter")
        _ <- counter.inc
        out <- counter.get
      } yield out

      test.unsafeRunSync must_=== 1D
    }
  }

  "Counter Labelled" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.labelled(cr, "boo", "Boo Counter", Sized("foo"), {s: String => Sized(s)})
      } yield counter

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.labelled(cr, "boo", "Boo Counter", Sized("foo"), {s: String => Sized(s)})
        _ <- counter.label("foo").inc
        out <- counter.label("foo").get
      } yield out

      test.unsafeRunSync must_=== 1D
    }
  }


}