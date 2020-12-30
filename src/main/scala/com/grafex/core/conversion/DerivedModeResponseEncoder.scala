package com.grafex.core
package conversion

import com.grafex.core.mode.{ ModeError, ModeRequest, ModeResponse }
import shapeless.Generic

trait DerivedModeResponseEncoder[RES] extends ModeResponseEncoder[RES]

object DerivedModeResponseEncoder {

  implicit def deriveEncoder[RES, Repr](
    implicit
    gen: Generic.Aux[RES, Repr],
    encodeRepr: ReprModeResponseEncoder[Repr]
  ): DerivedModeResponseEncoder[RES] = DerivedModeResponseEncoder.instance { (res: RES, req: ModeRequest) =>
    encodeRepr.apply(gen.to(res))(req)
  }

  def instance[RES](f: (RES, ModeRequest) => Either[ModeError, ModeResponse]): DerivedModeResponseEncoder[RES] =
    new DerivedModeResponseEncoder[RES] {
      override def apply(res: RES)(req: ModeRequest): Either[ModeError, ModeResponse] = f(res, req)
    }
}
