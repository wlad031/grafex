package com.grafex.core.conversion

import com.grafex.core.{ Mode, ModeError }
import shapeless.Generic

trait DerivedModeResponseEncoder[RES] extends ModeResponseEncoder[RES]

object DerivedModeResponseEncoder {

  implicit def deriveEncoder[RES, Repr](
    implicit
    gen: Generic.Aux[RES, Repr],
    encodeRepr: ReprModeResponseEncoder[Repr]
  ): DerivedModeResponseEncoder[RES] = DerivedModeResponseEncoder.instance { (res: RES, req: Mode.Request) =>
    encodeRepr.apply(gen.to(res))(req)
  }

  def instance[RES](f: (RES, Mode.Request) => Either[ModeError, Mode.Response]): DerivedModeResponseEncoder[RES] =
    new DerivedModeResponseEncoder[RES] {
      override def apply(res: RES)(req: Mode.Request): Either[ModeError, Mode.Response] = f(res, req)
    }
}
