package io.chrisdavenport.epimetheus

import cats.effect._
import org.specs2.mutable.Specification
import cats.effect.laws.util.TestContext
import shapeless._

class SummarySpec extends Specification {
  "Summary No Labels" should {
    "Register cleanly in the collector" in {
      implicit val ec = TestContext()
      implicit val T = ec.timer[IO]
      
      val test = for {
        cr <- CollectorRegistry.build[IO]
        s <- Summary.buildQuantiles[IO](cr, "boo", "Boo ", (0.5, 0.05))
      } yield s

      test.attempt.unsafeRunSync must beRight
    }
  }

  "Summary Labelled" should {
    "Register cleanly in the collector" in {
      implicit val ec = TestContext()
      implicit val T = ec.timer[IO]
      
      val test = for {
        cr <- CollectorRegistry.build[IO]
        s <- Summary.construct(cr, "boo", "Boo ", Sized("boo"), {s: String => Sized(s)}, (0.5, 0.05))
      } yield s

      test.attempt.unsafeRunSync must beRight
    }
  }
}