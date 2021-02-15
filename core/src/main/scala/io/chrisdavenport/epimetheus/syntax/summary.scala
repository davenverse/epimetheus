package io.chrisdavenport.epimetheus
package syntax

import cats.effect._
import scala.concurrent.duration._

trait summary {

  implicit class SummaryTimedOp[E, F[_]: MonadCancel[*[_], E]: Clock](
    private val s: Summary[F]
  ){
    def timed[A](fa: F[A], unit: TimeUnit): F[A] = 
      Summary.timed(s, fa, unit)

    def timedSeconds[A](fa: F[A]): F[A] = 
      Summary.timedSeconds(s, fa)
  }

}

object summary extends summary