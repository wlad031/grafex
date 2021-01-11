package com.grafex.core
package definitions
package generic

import com.grafex.core.definitions.action.{ InputSchema, OutputSchema }
import com.grafex.core.definitions.annotations.{ actionId, description }
import com.grafex.core.definitions.property._
import shapeless.Annotation

object semiauto extends implicits.all {

  def deriveInputSchema[A](
    implicit
    enc: FieldEncoder[A]
  ): InputSchema[A] = InputSchema(f("root", enc.encode, None)) // TODO: should it be "root" and "None"?

  def deriveOutputSchema[A](
    implicit
    enc: FieldEncoder[A]
  ): OutputSchema[A] = OutputSchema(f("root", enc.encode, None)) // TODO: should it be "root" and "None"?

  def deriveActionDefinition[A, IS : FieldEncoder, OS : FieldEncoder](
    implicit
    is: action.InputSchema[IS],
    os: action.OutputSchema[OS],
    actionIdA: Annotation[actionId, A],
    descA: Annotation[Option[description], A]
  ): action.Definition[A, IS, OS] = {
    val id = actionIdA()
    action.Definition(action.Id(id.name), is, os, descA().map(_.s))
  }

  // TODO: refactor this function
  private[generic] def f(name: String, p: Field, m: Option[FieldMetadata]): property.Definition = {
    p match {
      case BooleanField   => property.BooleanDefinition(name, m.flatMap(_.description))
      case StringField    => property.StringDefinition(name, m.flatMap(_.description))
      case IntField       => property.IntDefinition(name, m.flatMap(_.description))
      case FloatField     => property.FloatDefinition(name, m.flatMap(_.description))
      case OptionField(p) => property.OptionDefinition(f(name, p, m))
      case ListField(p)   => property.ListDefinition(name, f(name, p, None), m.flatMap(_.description))
      case EitherField(l, r) =>
        property.EitherDefinition(name, f(name, l, None), f(name, r, None), m.flatMap(_.description))
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
