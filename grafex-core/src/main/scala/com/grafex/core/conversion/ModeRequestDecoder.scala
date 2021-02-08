package com.grafex.core
package conversion

import cats.syntax.either._
import com.grafex.core.definitions.mode
import com.grafex.core.errors.{ InvalidRequest, UnknownAction }

trait ModeRequestDecoder[REQ] {
  def apply(req: ModeRequest): EitherE[REQ]
}

object ModeRequestDecoder {
  def instance[REQ](pf: PartialFunction[ModeRequest, EitherE[REQ]]): ModeRequestDecoder[REQ] =
    (req: ModeRequest) => pf.applyOrElse(req, (req: ModeRequest) => Left(UnknownAction(req.calls.head.actionId)))

  def instanceF[M, MInput](
    implicit
    modeDefinition: mode.BasicDefinition[M, MInput, _]
  ): ModeRequestDecoder[MInput] = { (req: ModeRequest) =>
    modeDefinition.actions
      .map(_.actionRequestDecoder)
      .reduce(_ orElse _)
      .applyOrElse(req, (r: ModeRequest) => InvalidRequest.UnsupportedAction(r.calls.head.actionId.name).asLeft)
  }
}
