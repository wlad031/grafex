package com.grafex.core
package conversion

import shapeless.{ :+:, CNil, Coproduct, Inl, Inr }

trait ReprModeResponseEncoder[MOut] extends ModeResponseEncoder[MOut]

object ReprModeResponseEncoder {

  implicit val cnilEnc: ReprModeResponseEncoder[CNil] = new ReprModeResponseEncoder[CNil]() {
    override def apply(v1: (ModeRequest, CNil)): EitherE[ModeResponse] = sys.error("Unreachable code")
  }

  implicit def coprodEnc[H, T <: Coproduct](
    implicit
    actionResponseEncoder: ActionResponseEncoder[H],
    tModeResponseEncoder: ReprModeResponseEncoder[T]
  ): ReprModeResponseEncoder[H :+: T] = new ReprModeResponseEncoder[H :+: T] {
    override def apply(v1: (ModeRequest, H :+: T)): EitherE[ModeResponse] = v1._2 match {
      case Inl(h) => actionResponseEncoder.apply((v1._1, h))
      case Inr(t) => tModeResponseEncoder.apply((v1._1, t))
    }
  }
}
