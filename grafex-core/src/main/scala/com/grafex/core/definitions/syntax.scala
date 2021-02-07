package com.grafex.core
package definitions

import com.grafex.core.conversion.ActionRequestDecoder
import com.grafex.core.definitions.mode.DecodableActionDefinition

object syntax {

  implicit class ActionDefinitionOps[A, AIn, AOut](actionDefinition: action.Definition[A, AIn, AOut]) {

    def asDecodable(
      implicit actionRequestDecoder: ActionRequestDecoder[AIn]
    ): DecodableActionDefinition[A, AIn, AOut] = {
      DecodableActionDefinition(actionDefinition, actionRequestDecoder)
    }
  }
}
