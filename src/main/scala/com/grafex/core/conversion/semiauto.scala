package com.grafex.core.conversion

import cats.instances.either._
import cats.syntax.bifunctor._
import com.grafex.core.{ InputType, Mode, OutputType, RequestFormatError }
import io.circe.syntax.EncoderOps
import shapeless.Lazy

object semiauto {

  final def deriveModeResponseEncoder[RES](
    implicit ev: Lazy[DerivedModeResponseEncoder[RES]]
  ): DerivedModeResponseEncoder[RES] = ev.value

  final def deriveOnlyJsonActionResponseEncoder[RES : io.circe.Encoder]: ActionResponseEncoder[RES] =
    ActionResponseEncoder.instance[RES]({
      case OutputType.Json => response => Right(Mode.Response(response.asJson.spaces2))
    })

  final def deriveOnlyJsonActionRequestDecoder[REQ : io.circe.Decoder]: ActionRequestDecoder[REQ] =
    ActionRequestDecoder.instance[REQ]({
      case InputType.Json =>
        (req: Mode.SingleCallRequest) => io.circe.parser.decode[REQ](req.body).leftMap(e => RequestFormatError(req, e))
    })
}
