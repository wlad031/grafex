package com.grafex.core
package conversion

import com.grafex.core.definitions.mode
import com.grafex.core.modeFoo.ModeError.UnknownAction
import com.grafex.core.modeFoo.{ ModeError, ModeRequest }

trait ModeRequestDecoder[REQ] {
  def apply(req: ModeRequest): Either[ModeError, REQ]
}

object ModeRequestDecoder {
  def instance[REQ](pf: PartialFunction[ModeRequest, Either[ModeError, REQ]]): ModeRequestDecoder[REQ] =
    (req: ModeRequest) => pf.applyOrElse(req, (req: ModeRequest) => Left(UnknownAction(req.calls.head.actionId)))

  def instanceF[REQ](implicit modeDefinition: mode.BasicDefinition): ModeRequestDecoder[REQ] = { (req: ModeRequest) =>
    modeDefinition.actions
      .find(_.actionDefinition.suitsFor(req.calls.head.actionId))
      .toRight(UnknownAction(req.calls.head.actionId): ModeError)
      .flatMap(r => r.actionRequestDecoder.decode(req).map(_.asInstanceOf[REQ])) // FIXME: unsafe operation
  }
}
