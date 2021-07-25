package dev.vgerasimov.grafex
package core
package conversion

trait ActionResponseEncoder[A] extends PartialFunction[(ModeRequest, A), EitherE[ModeResponse]] { self =>
  def asDefault: ActionResponseEncoder[A] = new ActionResponseEncoder[A] {
    override def isDefinedAt(x: (ModeRequest, A)): Boolean = true
    override def apply(x: (ModeRequest, A)): EitherE[ModeResponse] = self.apply(x)
  }
}

object ActionResponseEncoder {

  def instance[A](pf: PartialFunction[(ModeRequest, A), EitherE[ModeResponse]]): ActionResponseEncoder[A] =
    new ActionResponseEncoder[A] {
      override def isDefinedAt(x: (ModeRequest, A)): Boolean = pf.isDefinedAt(x)
      override def apply(v1: (ModeRequest, A)): EitherE[ModeResponse] = pf.apply(v1)
    }
}
