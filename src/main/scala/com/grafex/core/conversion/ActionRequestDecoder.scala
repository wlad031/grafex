package com.grafex.core.conversion

import com.grafex.core.{ InputType, Mode, ModeError, UnsupportedInputType }

trait ActionRequestDecoder[REQ] {
  def decode(inputType: InputType, request: Mode.SingleCallRequest): Either[ModeError, REQ]
}

object ActionRequestDecoder {
  def instance[A](
    pf: PartialFunction[InputType, Mode.SingleCallRequest => Either[ModeError, A]]
  ): ActionRequestDecoder[A] = { (in: InputType, req: Mode.SingleCallRequest) =>
    pf.applyOrElse(in, (in1: InputType) => (_: Mode.SingleCallRequest) => Left(UnsupportedInputType(in1)))(req)
  }
}
