package com.grafex.core
package conversion

import shapeless.Lazy
import cats.instances.either._
import cats.syntax.bifunctor._
import com.grafex.core.errors.InvalidRequest

trait JsonActionRequestDecoder[+AIn] extends ActionRequestDecoder[AIn]

object JsonActionRequestDecoder {

  import io.circe.parser.parse

  def instance[AIn](
    actionId: definitions.action.Id
  )(implicit jsonDecoder: Lazy[io.circe.Decoder[AIn]]): JsonActionRequestDecoder[AIn] = {
    new JsonActionRequestDecoder[AIn] {
      override protected def applicableFor: definitions.action.Id = actionId

      override def isDefinedAt(request: ModeRequest): Boolean =
        request.calls.head.actionId == actionId && {
          val options = request.options
          Set("json").contains(options.getOrElse("input", null))
        }

      override def apply(request: ModeRequest): EitherE[AIn] = {
        for {
          bodyJson <- parse(request.data.headOption match {
            case Some(value) => value
            case None        => "{}"
          }).leftMap(e => InvalidRequest.InvalidJson(e))
          bodyDec <- jsonDecoder.value.decodeJson(bodyJson).leftMap(e => InvalidRequest.InvalidTypeOfJson(e))
        } yield bodyDec
      }
    }
  }
}
