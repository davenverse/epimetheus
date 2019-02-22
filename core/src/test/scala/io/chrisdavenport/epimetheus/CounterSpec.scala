package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification

class CounterSpec extends Specification {

  "Counter" should {
    "Register cleanly in the collector" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.build[IO](cr, "boo", "Boo Counter")
      } yield counter

      test.attempt.unsafeRunSync must beRight
    }

    "Increase correctly" in {
      val test = for {
        cr <- CollectorRegistry.build[IO]
        counter <- Counter.build[IO](cr, "boo", "Boo Counter")
        _ <- counter.inc
        out <- counter.get
      } yield out

      test.unsafeRunSync must_=== 1D
    }
  }

}