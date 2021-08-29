package dev.vgerasimov.grafex
package core
package definitions

import cats.data.NonEmptyList

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
    * @tparam A the action type
    * @tparam AIn the type of input schema
    * @tparam AOut the type of output schema
    */
  final case class Definition[A, AIn, AOut](
    name: String,
    path: Path,
    input: InputSchema[AIn],
    output: OutputSchema[AOut],
    description: Option[String]
  ) {
    def suitsFor(requestPath: RequestPath): Option[Map[String, String]] = {
      def iter(path: Path, req: RequestPath, parsedParams: Map[String, String]) = {
        (path, req) match {
          case ((NonEmptyList(pathHead, pathTail)))
        }
      }
    }
  }

  object Definition {

    /** Summoner for [[Definition]] objects. */
    def instance[A, Input, Output](implicit ev: Definition[A, Input, Output]): Definition[A, Input, Output] = ev
  }
}
