package com.grafex.core
package conversion

import com.grafex.core.mode.ModeError.UnknownAction
import com.grafex.core.mode.{ ModeError, ModeRequest }

trait ModeRequestDecoder[REQ] {
  def apply(req: ModeRequest): Either[ModeError, REQ]
}

object ModeRequestDecoder {
  def instance[REQ](pf: PartialFunction[ModeRequest, Either[ModeError, REQ]]): ModeRequestDecoder[REQ] =
    (req: ModeRequest) => pf.applyOrElse(req, (req: ModeRequest) => Left(UnknownAction(req.calls.head.actionKey)))
}
