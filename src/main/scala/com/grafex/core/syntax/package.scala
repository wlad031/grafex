package com.grafex.core

import com.grafex.core.conversion.{ ActionRequestDecoder, ActionResponseEncoder }

package object syntax {
  implicit final class AbstractModeOps[A](private val value: A) extends AnyVal {
    def asAbstractModeResponse(outputType: OutputType)(
      implicit encoder: ActionResponseEncoder[A]
    ): Either[ModeError, Mode.Response] = encoder.encode(outputType, value)
  }

  implicit final class ActionRequestOps(private val value: Mode.SingleCallRequest) extends AnyVal {
    def asActionRequest[A](inputType: InputType)(
      implicit decoder: ActionRequestDecoder[A]
    ): Either[ModeError, A] = decoder.decode(inputType, value)
  }
}
