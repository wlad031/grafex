package dev.vgerasimov.grafex
package core
package definitions

/** This package contains everything that needs to describe mode action's input or output data types.
  *
  * It introduces two similar data types: [[property.Definition]] and [[property.Field]].
  *   - '''Definition''' is a part of other definition data type, such as [[action.Definition]] and
  *     [[mode.Definition]]. It contains all the information about given input/output data type field.
  *   - '''Field''' is internal representation of input/output data type field. It contains only
  *     type/name information and used only for automatic derivation of '''property definitions'''.
  *
  * @example {{{
  * val inputDefinition: property.Definition = property.StringDefinition(
  *   name = "lastName",
  *   description = Some("This is your last name")
  * )
  * }}}
  */
object property {

  // region Definitions

  /** Property definition is a description of action request/response parameters. */
  sealed trait Definition

  /** Represents definition for boolean parameter. */
  final case class BooleanDefinition(
    name: String,
    description: Option[String]
  ) extends Definition

  /** Represents definition for int parameter. */
  final case class IntDefinition(
    name: String,
    description: Option[String]
  ) extends Definition

  /** Represents definition for float parameter. */
  final case class FloatDefinition(
    name: String,
    description: Option[String]
  ) extends Definition

  /** Represents definition for string parameter. */
  final case class StringDefinition(
    name: String,
    description: Option[String]
  ) extends Definition

  /** Represents definition for any parameter which is optional (not required). */
  final case class OptionDefinition(
    item: Definition
  ) extends Definition

  /** Represents definition for list parameter. */
  final case class ListDefinition(
    name: String,
    itemsDefinition: Definition,
    description: Option[String]
  ) extends Definition

  /** Represents definition for complex object parameter. */
  final case class ObjectDefinition(
    name: String,
    fields: Map[String, Definition],
    description: Option[String]
  ) extends Definition

  final case class EitherDefinition(
    name: String,
    left: Definition,
    right: Definition,
    description: Option[String]
  ) extends Definition

  // endregion


  /** Represents metadata about some field, for example it's human-readable description. */
  final case class FieldMetadata(description: Option[String])

//  final class AnnotatedObjectField(m: Map[Symbol, (Field, Option[FieldMetadata])]) {
//    def fields: Map[Symbol, Field] = m.view.mapValues(_._1).toMap
//    val annotatedFields: Map[Symbol, (Field, Option[FieldMetadata])] = m
//  }

  // region Field encoders


//  trait AnnotatedObjectFieldEncoder[A] {
//    def encode: AnnotatedObjectField
//  }
//
//  object AnnotatedObjectFieldEncoder {
//    def instance[A](field: AnnotatedObjectField): AnnotatedObjectFieldEncoder[A] =
//      new AnnotatedObjectFieldEncoder[A] {
//        override val encode: AnnotatedObjectField = field
//      }
//  }

  trait FieldsMetadataEncoder[A] {
    def encode: Map[Symbol, FieldMetadata]
  }

  object FieldsMetadataEncoder {
    def instance[A](m: Map[Symbol, FieldMetadata]): FieldsMetadataEncoder[A] = new FieldsMetadataEncoder[A] {
      override val encode: Map[Symbol, FieldMetadata] = m
    }
  }

  // endregion
}
