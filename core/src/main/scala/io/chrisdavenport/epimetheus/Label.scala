package io.chrisdavenport.epimetheus

import cats._
import cats.implicits._

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

final class Label private(val getLabel: String) extends AnyVal {
  def ++(that: Label): Label = new Label(getLabel |+| that.getLabel)
  def suffix(s: Label.Suffix): Label = new Label(getLabel |+| s.getSuffix)
}

object Label {
  implicit val labelInstances: Show[Label] with Semigroup[Label] with Eq[Label] with Order[Label] =
    new Show[Label] with Semigroup[Label] with Eq[Label] with Order[Label]{
      // Members declared in cats.Show.ContravariantShow
      def show(t: Label): String = t.getLabel
      // Members declared in cats.kernel.Semigroup
      def combine(x: Label, y: Label): Label = x ++ y
      // Members declared in cats.kernel.Order
      override def compare(x: Label, y: Label): Int =
        Order[String].compare(x.getLabel, y.getLabel)
      // Members declared in cats.kernel.Eq
      override def eqv(x: Label, y: Label): Boolean =
        Eq[String].eqv(x.getLabel, y.getLabel)
    }

  /** See [[https://prometheus.io/docs/concepts/data_model/#metric-names-and-labels]] */
  private val reg = "([a-zA-Z_][a-zA-Z0-9_]*)".r

  private[Label] class Macros(val c: whitebox.Context) {
    import c.universe._
    def labelLiteral(s: c.Expr[String]): Tree =
      s.tree match {
        case Literal(Constant(s: String))=>
            impl(s)
            .fold(
              e => c.abort(c.enclosingPosition, e.getMessage),
              _ =>
                q"_root_.io.chrisdavenport.epimetheus.Label.impl($s).fold(throw _, _root_.scala.Predef.identity)"
            )
        case _ =>
          c.abort(
            c.enclosingPosition,
            s"This method uses a macro to verify that a Label literal is valid. Use Label.impl if you have a dynamic value you want to parse as a label."
          )
      }
  }

  def impl(s: String): Either[IllegalArgumentException, Label] = s match {
    case reg(string) => Either.right(new Label(string))
    case _ => Either.left(
      new IllegalArgumentException(
        s"Input String - $s does not match regex - $reg"
      )
    )
  }
  def implF[F[_]: ApplicativeError[?[_], Throwable]](s: String): F[Label] = {
    impl(s).liftTo[F]
  }

  def apply(s: String): Label = macro Macros.labelLiteral

  final class Suffix private(val getSuffix: String) extends AnyVal {
    def ++(that: Suffix): Suffix = new Suffix(getSuffix |+| that.getSuffix)
  }
  object Suffix {

    implicit val labelInstances: Show[Suffix] with Semigroup[Suffix] =
      new Show[Suffix] with Semigroup[Suffix]{
        // Members declared in cats.Show.ContravariantShow
        def show(t: Suffix): String = t.getSuffix
        // Members declared in cats.kernel.Semigroup
        def combine(x: Suffix, y: Suffix): Suffix = x ++ y
      }

    private[Suffix] class Macros(val c: whitebox.Context) {
      import c.universe._
      def suffixLiteral(s: c.Expr[String]): Tree =
        s.tree match {
          case Literal(Constant(s: String))=>
              impl(s)
              .fold(
                e => c.abort(c.enclosingPosition, e.getMessage),
                _ =>
                  q"_root_.io.chrisdavenport.epimetheus.Label.Suffix.impl($s).fold(throw _, _root_.scala.Predef.identity)"
              )
          case _ =>
            c.abort(
              c.enclosingPosition,
              s"This method uses a macro to verify that a Label.Suffix literal is valid. Use Label.Suffix.impl if you have a dynamic value you want to parse as a suffix."
            )
        }
    }

    private val sufreg = "([a-zA-Z0-9_]*)".r

    def impl(s: String): Either[IllegalArgumentException, Suffix] = s match {
      case sufreg(string) => Either.right(new Suffix(string))
      case _ => Either.left(
        new IllegalArgumentException(
          s"Input String - $s does not match regex - $sufreg"
        )
      )
    }

    def implF[F[_]: ApplicativeError[?[_], Throwable]](s: String): F[Suffix] = {
      impl(s).liftTo[F]
    }

    def apply(s: String): Suffix = macro Macros.suffixLiteral

  }

}
