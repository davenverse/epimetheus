package io.chrisdavenport.epimetheus

trait ShapelessPolyfill {

  type Nat = shapeless.Nat

  type Sized[+Repr,L <: Nat] = shapeless.Sized[Repr, L]

  val Sized = shapeless.Sized

}
