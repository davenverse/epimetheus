package io.chrisdavenport.epimetheus
package syntax

import cats.effect._

trait summary {

  implicit class SummaryTimedOp[E, F[_]: Bracket[?[_],E]: Clock](
    private val s: Summary[F]
  ){
    def timed[A](fa: F[A]): F[A] = 
      Summary.timed(s, fa)
  }

}

object summary extends summary