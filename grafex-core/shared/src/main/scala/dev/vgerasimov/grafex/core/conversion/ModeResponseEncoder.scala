package dev.vgerasimov.grafex
package core
package conversion

trait ModeResponseEncoder[A] extends (((ModeRequest, A)) => EitherE[ModeResponse])

object ModeResponseEncoder {
  def instance[RES](f: (ModeRequest, RES) => EitherE[ModeResponse]): ModeResponseEncoder[RES] =
    new ModeResponseEncoder[RES] {
      override def apply(v1: (ModeRequest, RES)): EitherE[ModeResponse] = f(v1._1, v1._2)
    }
}
