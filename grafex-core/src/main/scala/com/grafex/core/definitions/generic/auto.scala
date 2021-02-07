package com.grafex.core
package definitions
package generic

import shapeless.{ Annotation, Lazy }
import com.grafex.core.definitions.action.{ InputSchema, OutputSchema }
import com.grafex.core.definitions.annotations.{ actionId, description }
import com.grafex.core.definitions.property.FieldEncoder

object auto {

  implicit def deriveInputSchema[A](implicit enc: FieldEncoder[A]): InputSchema[A] = semiauto.deriveInputSchema
  implicit def deriveOutputSchema[A](implicit enc: FieldEncoder[A]): OutputSchema[A] = semiauto.deriveOutputSchema

  implicit def deriveActionDefinition[A, AIn, AOut](
    implicit
    is: Lazy[action.InputSchema[AIn]],
    os: Lazy[action.OutputSchema[AOut]],
    actionIdA: Annotation[actionId, A],
    descA: Annotation[Option[description], A]
  ): action.Definition[A, AIn, AOut] = semiauto.deriveActionDefinition
}
