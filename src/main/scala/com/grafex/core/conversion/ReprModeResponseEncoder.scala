package com.grafex.core.conversion

import com.grafex.core.{ Mode, ModeError }
import shapeless.{ :+:, CNil, Coproduct, Inl, Inr }

trait ReprModeResponseEncoder[RES] extends ModeResponseEncoder[RES]

object ReprModeResponseEncoder {

  implicit val cnilEnc: ReprModeResponseEncoder[CNil] =
    ReprModeResponseEncoder.instance { (_: CNil, _: Mode.Request) => sys.error("Unreachable code") }

  implicit def coprodEnc[H, T <: Coproduct](
    implicit
    actionResponseEncoder: ActionResponseEncoder[H],
    tModeResponseEncoder: ReprModeResponseEncoder[T]
  ): ReprModeResponseEncoder[H :+: T] = ReprModeResponseEncoder.instance { (res: H :+: T, req: Mode.Request) =>
    res match {
      case Inl(h) => actionResponseEncoder.encode(req.outputType, h)
      case Inr(t) => tModeResponseEncoder.apply(t)(req)
    }
  }

  private def instance[RES](f: (RES, Mode.Request) => Either[ModeError, Mode.Response]): ReprModeResponseEncoder[RES] =
    new ReprModeResponseEncoder[RES] {
      override def apply(res: RES)(req: Mode.Request): Either[ModeError, Mode.Response] = f(res, req)
    }
}
