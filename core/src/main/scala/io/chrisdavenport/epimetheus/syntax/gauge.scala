package io.chrisdavenport.epimetheus.syntax

import io.chrisdavenport.epimetheus.Gauge
import cats.effect._

trait gauge {
  implicit class BracketGaugeOps[E, F[_]: Bracket[?[_], E], A](private val gauge: Gauge[F]){
    def incIn(fa: F[A]): F[A] = Gauge.incIn(gauge, fa)
    def incByIn(fa: F[A], i: Double): F[A] = Gauge.incByIn(gauge, fa, i)
    def decIn(fa: F[A]): F[A] = Gauge.decIn(gauge, fa)
    def decByIn(fa: F[A], i: Double): F[A] = Gauge.decByIn(gauge, fa, i)
  }

}

object gauge extends gauge