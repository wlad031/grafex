package com.grafex.core
package definitions

import com.grafex.core.definitions.annotations.{ actionId, description }
import com.grafex.core.definitions.property._
import shapeless.Annotation

object action {

  final case class Id(name: String)

  final case class InputSchema[A](propertyDefinition: property.Definition)
  final case class OutputSchema[A](propertyDefinition: property.Definition)

  final case class Definition[A, IS, OS](
    id: Id,
    input: InputSchema[IS],
    output: OutputSchema[OS],
    description: Option[String]
  ) {
    def suitsFor(id: Id): Boolean = this.id == id
  }

  object Definition {
    def instance[A, IS, OS](implicit ev: Definition[A, IS, OS]): Definition[A, IS, OS] = ev


  }
}
