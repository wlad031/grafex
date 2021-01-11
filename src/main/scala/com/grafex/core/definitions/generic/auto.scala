package com.grafex.core
package definitions
package generic

import com.grafex.core.definitions.action.{ InputSchema, OutputSchema }
import com.grafex.core.definitions.annotations.{ actionId, description }
import com.grafex.core.definitions.property.FieldEncoder
import shapeless.Annotation

object auto extends implicits.all {

  implicit def deriveInputSchema[A](implicit enc: FieldEncoder[A]): InputSchema[A] = semiauto.deriveInputSchema
  implicit def deriveOutputSchema[A](implicit enc: FieldEncoder[A]): OutputSchema[A] = semiauto.deriveOutputSchema

  implicit def deriveActionDefinition[A, IS : FieldEncoder, OS : FieldEncoder](
    implicit
    is: action.InputSchema[IS],
    os: action.OutputSchema[OS],
    actionIdA: Annotation[actionId, A],
    descA: Annotation[Option[description], A]
  ): action.Definition[A, IS, OS] = semiauto.deriveActionDefinition
}
