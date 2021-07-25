package dev.vgerasimov.grafex
package core
package conversion
package generic

import shapeless.Lazy
import dev.vgerasimov.grafex.core.definitions.mode

import scala.reflect.runtime.{ universe => un }

object semiauto {

  final def deriveModeResponseEncoder[MOut](
    implicit ev: Lazy[DerivedModeResponseEncoder[MOut]]
  ): DerivedModeResponseEncoder[MOut] = ev.value

  final def deriveModeRequestDecoder[M, MIn](
    implicit modeDefinition: mode.BasicDefinition[M, MIn, _]
  ): ModeRequestDecoder[MIn] = ModeRequestDecoder.instanceF

  final def deriveJsonActionResponseEncoder[AOut](
    implicit jsonEncoder: Lazy[io.circe.Encoder[AOut]]
  ): JsonActionResponseEncoder[AOut] =
    JsonActionResponseEncoder.instance[AOut] { (_, data) => ModeResponse.Ok(data) }

  final def deriveJsonActionErrorEncoder[AOut <: ErrorMeta : un.TypeTag](
    implicit jsonEncoder: Lazy[io.circe.Encoder[AOut]]
  ): JsonActionResponseEncoder[AOut] =
    JsonActionResponseEncoder.instance[AOut] { (response, data) =>
      ModeResponse.Error(
        data,
        response.code,
        options = Map(
          "errorCode" -> response.code.toString,
          "errorName" -> un.typeOf[AOut].typeSymbol.name.toString
        )
      )
    }

  final def deriveJsonActionRequestDecoder[AIn](actionId: definitions.action.Id)(
    implicit
    jsonDecoder: Lazy[io.circe.Decoder[AIn]]
  ): JsonActionRequestDecoder[AIn] = {
    JsonActionRequestDecoder.instance[AIn](actionId)
  }

  final def deriveJsonActionRequestDecoder[AIn](
    implicit
    definition: definitions.action.Definition[_, AIn, _],
    jsonDecoder: Lazy[io.circe.Decoder[AIn]]
  ): JsonActionRequestDecoder[AIn] = {
    JsonActionRequestDecoder.instance[AIn](definition.id)
  }
}
