package io.chrisdavenport.epimetheus
package syntax

import cats.effect._

trait histogram {

  implicit class HistogramTimedOp[E, F[_]: Bracket[?[_],E]: Clock](
    private val h: Histogram[F]
  ){
    def timed[A](fa: F[A]): F[A] = 
      Histogram.timed(h, fa)
  }

}

object histogram extends histogram