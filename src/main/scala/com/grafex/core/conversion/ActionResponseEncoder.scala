package com.grafex.core.conversion

import com.grafex.core.{ Mode, ModeError, OutputType, UnsupportedOutputType }

trait ActionResponseEncoder[RES] {
  def encode(outputType: OutputType, res: RES): Either[ModeError, Mode.Response]
}

object ActionResponseEncoder {
  def instance[RES](
    pf: PartialFunction[OutputType, RES => Either[ModeError, Mode.Response]]
  ): ActionResponseEncoder[RES] = { (out: OutputType, res: RES) =>
    pf.applyOrElse(out, (out1: OutputType) => (_: RES) => Left(UnsupportedOutputType(out1)))(res)
  }
}
