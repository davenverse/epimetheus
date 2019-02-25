package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

final class Name private(val getName: String){
  def ++(that: Name): Name = new Name(getName |+| that.getName)
}

object Name {
  implicit val nameInstances: Show[Name] with Semigroup[Name] = 
    new Show[Name] with Semigroup[Name]{
      // Members declared in cats.Show.ContravariantShow
      def show(t: Name): String = t.getName
      // Members declared in cats.kernel.Semigroup
      def combine(x: Name, y: Name): Name = x ++ y
    }

  private val reg = "([a-zA-Z_:][a-zA-Z0-9_:]*)".r

  private[Name] class Macros(val c: whitebox.Context) {
    import c.universe._
    def nameLiteral(s: c.Expr[String]): Tree =
      s.tree match {
        case Literal(Constant(s: String))=>
            impl(s)
            .fold(
              e => c.abort(c.enclosingPosition, e.getMessage),
              _ =>
                q"_root_.io.chrisdavenport.epimetheus.Name.impl($s).fold(throw _, _root_.scala.Predef.identity)"
            )
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"This method uses a macro to verify that a Name literal is valid. Use Name.impl if you have a dynamic value you want to parse as a name."
          )
      }
  }

  def impl(s: String): Either[IllegalArgumentException, Name] = s match {
    case reg(string) => Either.right(new Name(string))
    case _ => Either.left(
      new IllegalArgumentException(
        s"Input String - $s does not match regex - ([a-zA-Z_:][a-zA-Z0-9_:]*)"
      )
    )
  }
  def implF[F[_]: ApplicativeError[?[_], Throwable]](s: String): F[Name] = {
    impl(s).liftTo[F]
  }

  def apply(s: String): Name = macro Macros.nameLiteral

}