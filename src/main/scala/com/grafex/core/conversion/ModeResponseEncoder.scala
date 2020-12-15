package com.grafex.core.conversion

import com.grafex.core.{ Mode, ModeError }

trait ModeResponseEncoder[RES] {
  def apply(res: RES)(req: Mode.Request): Either[ModeError, Mode.Response]
}

object ModeResponseEncoder {
  def instance[RES](f: (RES, Mode.Request) => Either[ModeError, Mode.Response]): ModeResponseEncoder[RES] =
    new ModeResponseEncoder[RES] {
      override def apply(res: RES)(req: Mode.Request): Either[ModeError, Mode.Response] = f(res, req)
    }
}
