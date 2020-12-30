package com.grafex.core
package conversion

import com.grafex.core.mode.ModeError.InvalidRequest
import com.grafex.core.mode.{ ModeError, ModeRequest }

trait ActionRequestDecoder[REQ] {
  def decode(request: ModeRequest): Either[ModeError, REQ]
}

object ActionRequestDecoder {
  def instance[A](
    pf: PartialFunction[ModeRequest, Either[ModeError, A]]
  ): ActionRequestDecoder[A] = { (req: ModeRequest) =>
    pf.applyOrElse(req, (_: ModeRequest) => Left(InvalidRequest.UnsupportedInputType(req)))
  }
}
