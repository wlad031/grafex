package com.grafex.core.jsonschema

import com.grafex.core.jsonschema.JsonSchema.NamedDefinition
import enumeratum.{ Enum, EnumEntry }
import io.circe._
import io.circe.syntax._

import scala.annotation.implicitNotFound
import scala.reflect.runtime.{ universe => ru }
import scala.util.matching.Regex

@implicitNotFound(
  "Cannot derive a JsonSchema for ${A}. Please verify that instances can be derived for all its fields"
)
trait JsonSchema[A] extends JsonSchema.HasRef {
  def id: String
  def inline: Boolean
  def relatedDefinitions: Set[JsonSchema.Definition]
  def fieldDefinitions: Set[JsonSchema.NamedDefinition] = relatedDefinitions.collect { case d: NamedDefinition => d }

  def jsonObject: JsonObject
  def asJson: Json = jsonObject.asJson

  def asObjectRef: JsonObject = JsonObject.singleton("$ref", s"#/definitions/$id".asJson)
  def asJsonRef: Json = asObjectRef.asJson

  def NamedDefinition(fieldName: String): NamedDefinition = JsonSchema.NamedDefinition(
    id,
    fieldName,
    relatedDefinitions.map(_.asRef),
    asJson
  )

  lazy val definition: JsonSchema.UnnamedDefinition =
    JsonSchema.UnnamedDefinition(id, relatedDefinitions.map(_.asRef), asJson)

  def definitions: Set[JsonSchema.Definition] = relatedDefinitions + definition
}

object JsonSchema
    extends Primitives
    with com.grafex.core.jsonschema.derive.HListInstances
    with com.grafex.core.jsonschema.derive.CoprodInstances {

  trait HasRef {
    def id: String
    def asRef: Ref = TypeRef(id, None)
    def asArrayRef: Ref = ArrayRef(id, None)
  }

  sealed trait Definition extends HasRef {
    def id: String
    def json: Json
    def relatedRefs: Set[Ref]
  }

  case class UnnamedDefinition(
    id: String,
    relatedRefs: Set[Ref],
    json: Json
  ) extends Definition

  case class NamedDefinition(
    id: String,
    fieldName: String,
    relatedRefs: Set[Ref],
    json: Json
  ) extends Definition {
    override def asRef: TypeRef = TypeRef(id, Some(fieldName))
    override def asArrayRef: ArrayRef = ArrayRef(id, Some(fieldName))
  }

  sealed trait Ref {
    def id: String
    def fieldName: Option[String]
  }

  case class TypeRef(
    id: String,
    fieldName: Option[String]
  ) extends Ref
  object TypeRef {
    def apply(definition: Definition): TypeRef = TypeRef(definition.id, None)
    def apply(schema: JsonSchema[_]): TypeRef = TypeRef(schema.id, None)
  }

  case class ArrayRef(
    id: String,
    fieldName: Option[String]
  ) extends Ref
  object ArrayRef {
    def apply(definition: Definition): ArrayRef = ArrayRef(definition.id, None)
    def apply(schema: JsonSchema[_]): ArrayRef = ArrayRef(schema.id, None)
  }

  trait PatternProperty[K] {
    def regex: Regex
  }

  object PatternProperty {
    def fromRegex[K](r: Regex): PatternProperty[K] = new PatternProperty[K] { override val regex: Regex = r }
    implicit def intPatternProp: PatternProperty[Int] = fromRegex[Int]("[0-9]*".r)
    implicit def wildcard[K]: PatternProperty[K] = fromRegex[K](".*".r)
  }

  def instance[A](
    obj: => JsonObject
  )(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id: String = tag.tpe.typeSymbol.fullName
    override def inline = false
    override def jsonObject: JsonObject = obj
    override def relatedDefinitions: Set[Definition] = Set.empty
  }

  def functorInstance[F[_], A](
    obj: => JsonObject
  )(implicit tag: ru.WeakTypeTag[A]): JsonSchema[F[A]] = new JsonSchema[F[A]] {
    override def id: String = tag.tpe.typeSymbol.fullName
    override def inline = false
    override def jsonObject: JsonObject = obj
    override def relatedDefinitions: Set[Definition] = Set.empty
  }

  def instanceAndRelated[A](
    pair: => (JsonObject, Set[Definition])
  )(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id: String = tag.tpe.typeSymbol.fullName
    override def inline = false
    override def jsonObject: JsonObject = pair._1
    override def relatedDefinitions: Set[Definition] = pair._2
  }

  def inlineInstance[A](
    obj: => JsonObject
  )(implicit tag: ru.WeakTypeTag[A]): JsonSchema[A] = new JsonSchema[A] {
    override def id: String = tag.tpe.typeSymbol.fullName
    override def inline = true
    override def relatedDefinitions: Set[Definition] = Set.empty
    override def jsonObject: JsonObject = obj
  }

  def enum[A : ru.WeakTypeTag](values: Seq[String]): JsonSchema[A] =
    inlineInstance(Map("enum" -> values.asJson).asJsonObject)
  def enum[A <: Enumeration : ru.WeakTypeTag](a: A): JsonSchema[A] =
    enum[A](a.values.map(_.toString).toList)
  def enum[E <: EnumEntry](e: Enum[E])(implicit ev: ru.WeakTypeTag[E]): JsonSchema[E] =
    enum[E](e.values.map(_.entryName))

  def deriveEnum[A](implicit ev: PlainEnum[A], tag: ru.WeakTypeTag[A]): JsonSchema[A] = enum[A](ev.ids)
  def deriveFor[A](implicit ev: JsonSchema[A]): JsonSchema[A] = ev
}