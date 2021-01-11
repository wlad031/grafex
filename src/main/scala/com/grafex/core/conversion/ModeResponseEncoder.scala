package com.grafex.core
package conversion

import com.grafex.core.modeFoo.{ ModeError, ModeRequest, ModeResponse }

trait ModeResponseEncoder[RES] {
  def apply(res: RES)(req: ModeRequest): Either[ModeError, ModeResponse]
}

object ModeResponseEncoder {
  def instance[RES](f: (RES, ModeRequest) => Either[ModeError, ModeResponse]): ModeResponseEncoder[RES] =
    new ModeResponseEncoder[RES] {
      override def apply(res: RES)(req: ModeRequest): Either[ModeError, ModeResponse] = f(res, req)
    }
}
