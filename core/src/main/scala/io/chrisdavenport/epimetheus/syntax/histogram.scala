package io.chrisdavenport.epimetheus
package syntax

import cats.effect._
import scala.concurrent.duration.TimeUnit

trait histogram {

  implicit class HistogramTimedOp[E, F[_]: Bracket[?[_],E]: Timer](
    private val h: Histogram[F]
  ){
    def timed[A](f: F[A], unit: TimeUnit): F[A] = 
      Histogram.timed(h, f, unit)
  }

}

object histogram extends histogram