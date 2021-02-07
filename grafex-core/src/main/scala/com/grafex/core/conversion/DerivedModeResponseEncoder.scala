package com.grafex.core
package conversion

import shapeless.Generic

trait DerivedModeResponseEncoder[MOut] extends ModeResponseEncoder[MOut]

object DerivedModeResponseEncoder {

  implicit def deriveEncoder[MOut, Repr](
    implicit
    gen: Generic.Aux[MOut, Repr],
    encodeRepr: ReprModeResponseEncoder[Repr]
  ): DerivedModeResponseEncoder[MOut] = new DerivedModeResponseEncoder[MOut] {
    override def apply(v1: (ModeRequest, MOut)): EitherE[ModeResponse] = encodeRepr.apply((v1._1, gen.to(v1._2)))
  }
}
