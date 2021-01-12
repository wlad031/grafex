package com.grafex.core
package conversion

import com.grafex.core.ModeError
import shapeless.{ :+:, CNil, Coproduct, Inl, Inr }

trait ReprModeResponseEncoder[RES] extends ModeResponseEncoder[RES]

object ReprModeResponseEncoder {

  implicit val cnilEnc: ReprModeResponseEncoder[CNil] =
    ReprModeResponseEncoder.instance { (_: CNil, _: ModeRequest) => sys.error("Unreachable code") }

  implicit def coprodEnc[H, T <: Coproduct](
    implicit
    actionResponseEncoder: ActionResponseEncoder[H],
    tModeResponseEncoder: ReprModeResponseEncoder[T]
  ): ReprModeResponseEncoder[H :+: T] = ReprModeResponseEncoder.instance { (res: H :+: T, req: ModeRequest) =>
    res match {
      case Inl(h) => actionResponseEncoder.encode(req.outputType, h)
      case Inr(t) => tModeResponseEncoder.apply(t)(req)
    }
  }

  private def instance[RES](f: (RES, ModeRequest) => Either[ModeError, ModeResponse]): ReprModeResponseEncoder[RES] =
    new ReprModeResponseEncoder[RES] {
      override def apply(res: RES)(req: ModeRequest): Either[ModeError, ModeResponse] = f(res, req)
    }
}
