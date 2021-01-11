package com.grafex.core
package definitions

import com.grafex.core.conversion.ActionRequestDecoder
import com.grafex.core.definitions.mode.DecodableActionDefinition

object syntax {

  implicit class ActionDefinitionOps[A, IS, OS](actionDefinition: action.Definition[A, IS, OS]) {
    def asDecodable(implicit actionRequestDecoder: ActionRequestDecoder[IS]): DecodableActionDefinition[A, IS, OS] = {
      DecodableActionDefinition(actionDefinition, actionRequestDecoder)
    }
  }
}
