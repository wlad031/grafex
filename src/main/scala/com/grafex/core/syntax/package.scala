package com.grafex.core

import com.grafex.core.conversion.{ ActionRequestDecoder, ActionResponseEncoder }
import com.grafex.core.mode.{ ModeError, ModeRequest, ModeResponse }

package object syntax {
  implicit final class AbstractModeOps[A](private val value: A) extends AnyVal {
    def asAbstractModeResponse(outputType: OutputType)(
      implicit encoder: ActionResponseEncoder[A]
    ): Either[ModeError, ModeResponse] = encoder.encode(outputType, value)
  }

  implicit final class ActionRequestOps(private val value: ModeRequest) extends AnyVal {
    def asActionRequest[A](
      implicit decoder: ActionRequestDecoder[A]
    ): Either[ModeError, A] = decoder.decode(value)
  }
}
