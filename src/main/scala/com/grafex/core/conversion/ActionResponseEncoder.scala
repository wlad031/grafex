package com.grafex.core
package conversion

import com.grafex.core.modeFoo.ModeError.InvalidRequest
import com.grafex.core.modeFoo.{ ModeError, ModeResponse }

trait ActionResponseEncoder[RES] {
  def encode(outputType: OutputType, res: RES): Either[ModeError, ModeResponse]
}

object ActionResponseEncoder {
  def instance[RES](
    pf: PartialFunction[OutputType, RES => Either[ModeError, ModeResponse]]
  ): ActionResponseEncoder[RES] = { (out: OutputType, res: RES) =>
    pf.applyOrElse(out, (out1: OutputType) => (_: RES) => Left(InvalidRequest.UnsupportedOutputType(out1)))(res)
  }
}
