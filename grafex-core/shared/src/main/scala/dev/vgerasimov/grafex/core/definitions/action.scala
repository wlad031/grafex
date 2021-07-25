package dev.vgerasimov.grafex
package core
package definitions

object action {

  final case class Id(name: String)

  // TODO: improve this doc
  /**
    *
    * @param propertyDefinition
    * @tparam A the type this schema belongs to
    */
  final case class InputSchema[A](propertyDefinition: property.Definition)

  // TODO: improve this doc
  /**
    *
    * @param propertyDefinition
    * @tparam A the type this schema belongs to
    */
  final case class OutputSchema[A](propertyDefinition: property.Definition)

  // TODO: improve this doc
  /**
    *
    * @param id
    * @param input
    * @param output
    * @param description
    * @tparam A the type of action
    * @tparam Input the type of input schema
    * @tparam Output the type of output schema
    */
  final case class Definition[A, Input, Output](
    id: Id,
    input: InputSchema[Input],
    output: OutputSchema[Output],
    description: Option[String]
  ) {
    def suitsFor(id: Id): Boolean = this.id == id
  }

  object Definition {

    /** Summoner for [[Definition]] objects. */
    def instance[A, Input, Output](implicit ev: Definition[A, Input, Output]): Definition[A, Input, Output] = ev
  }
}
