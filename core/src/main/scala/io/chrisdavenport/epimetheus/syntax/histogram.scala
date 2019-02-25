package io.chrisdavenport.epimetheus
package syntax

import cats.effect._
import scala.concurrent.duration._

trait histogram {

  implicit class HistogramTimedOp[E, F[_]: Bracket[?[_],E]: Clock](
    private val h: Histogram[F]
  ){
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Histogram.timed(h, fa, unit)

    def timedSeconds[A](fa: F[A]): F[A] =
      Histogram.timedSeconds(h, fa)
  }

}

object histogram extends histogram