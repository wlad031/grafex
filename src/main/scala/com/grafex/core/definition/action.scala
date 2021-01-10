package com.grafex.core
package definition

import com.grafex.core.definition.annotations.{ actionId, description }
import com.grafex.core.definition.property._
import shapeless.Annotation

object action {

  final case class Id(name: String)

  final case class InputSchema(propertyDefinition: property.Definition)

  object InputSchema {
    implicit def derive[A](
      implicit
      enc: FieldEncoder[A]
    ): InputSchema = InputSchema(f("root", enc.encode, None))
  }

  final case class OutputSchema(propertyDefinition: property.Definition)

  object OutputSchema {
    implicit def derive[A](
      implicit
      enc: FieldEncoder[A]
    ): OutputSchema = OutputSchema(f("root", enc.encode, None))
  }

  final case class Definition(
    id: Id,
    input: InputSchema,
    output: OutputSchema,
    description: Option[String]
  )

  object Definition {
    def derive[A, IS : FieldEncoder, OS : FieldEncoder](
      implicit
      actionIdA: Annotation[actionId, A],
      descA: Annotation[Option[description], A]
    ): action.Definition = {
      val is = com.grafex.core.definition.action.InputSchema.derive[IS]
      val os = com.grafex.core.definition.action.OutputSchema.derive[OS]
      val id = actionIdA()
      action.Definition(action.Id(id.name), is, os, descA().map(_.s))
    }
  }

  private def f(name: String, p: Field, m: Option[FieldMetadata]): property.Definition = {
    p match {
      case BooleanField   => property.BooleanDefinition(name, m.flatMap(_.description))
      case StringField    => property.StringDefinition(name, m.flatMap(_.description))
      case IntField       => property.IntDefinition(name, m.flatMap(_.description))
      case FloatField     => property.FloatDefinition(name, m.flatMap(_.description))
      case OptionField(p) => property.OptionDefinition(f(name, p, m))
      case ListField(p)   => property.ListDefinition(name, f(name, p, None), m.flatMap(_.description))
      case EitherField(l, r) => property.EitherDefinition(name, f(name, l, None), f(name, r, None), m.flatMap(_.description))
      case op: ObjectField =>
        op match {
          case wm: AnnotatedObjectField =>
            property.ObjectDefinition(name, op.fields.map({
              case (s, p) => (s.name, f(s.name, p, wm.annotatedFields.get(s).flatMap(_._2)))
            }), m.flatMap(_.description))
          case _ =>
            property.ObjectDefinition(name, op.fields.map({
              case (s, p) => (s.name, f(s.name, p, None))
            }), m.flatMap(_.description))
        }
    }
  }
}
