package com.grafex.core
package conversion

import cats.instances.either._
import cats.syntax.bifunctor._
import ModeError.RequestFormatError
import com.grafex.core.ModeResponse
import io.circe.syntax.EncoderOps
import shapeless.Lazy

object semiauto {

  final def deriveModeResponseEncoder[RES](
    implicit ev: Lazy[DerivedModeResponseEncoder[RES]]
  ): DerivedModeResponseEncoder[RES] = ev.value

  final def deriveOnlyJsonActionResponseEncoder[RES : io.circe.Encoder]: ActionResponseEncoder[RES] =
    ActionResponseEncoder.instance[RES]({
      case OutputType.Json => response => Right(ModeResponse.Json(response.asJson))
    })

  final def deriveOnlyJsonActionRequestDecoder[REQ](
    implicit jsonDecoder: Lazy[io.circe.Decoder[REQ]]
  ): ActionRequestDecoder[REQ] =
    ActionRequestDecoder.instance[REQ]({
      case req @ ModeRequest.Json(_, _, body) =>
        jsonDecoder.value.decodeJson(body).leftMap(e => RequestFormatError(req, e))
    })
}
