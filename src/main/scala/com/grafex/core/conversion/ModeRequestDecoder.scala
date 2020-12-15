package com.grafex.core.conversion

import com.grafex.core.{ Mode, ModeError, UnknownAction }

trait ModeRequestDecoder[REQ] {
  def apply(req: Mode.SingleCallRequest): Either[ModeError, REQ]
}

object ModeRequestDecoder {
  //noinspection ConvertExpressionToSAM
  def instance[REQ](pf: PartialFunction[Mode.SingleCallRequest, Either[ModeError, REQ]]): ModeRequestDecoder[REQ] =
    new ModeRequestDecoder[REQ] {
      def apply(req: Mode.SingleCallRequest): Either[ModeError, REQ] =
        pf.applyOrElse(req, (req: Mode.SingleCallRequest) => Left(UnknownAction(req.call.actionKey)))
    }
}
