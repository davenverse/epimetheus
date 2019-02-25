package io.chrisdavenport.epimetheus
package syntax

import cats.effect._
import scala.concurrent.duration.TimeUnit

trait summary {

  implicit class SummaryTimedOp[E, F[_]: Bracket[?[_],E]: Timer](
    private val s: Summary[F]
  ){
    def timed[A](f: F[A], unit: TimeUnit): F[A] = 
      Summary.timed(s, f, unit)
  }

}

object summary extends summary