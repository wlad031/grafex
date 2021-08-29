package dev.vgerasimov.grafex
package core
package definitions

import shapeless.ops.hlist.{ToTraversable, Zip}
import shapeless.ops.record.Keys
import shapeless.{::, Annotation, Annotations, HList, HNil, LabelledGeneric}
import dev.vgerasimov.grafex.core.definitions.action.{InputSchema, OutputSchema}
import dev.vgerasimov.grafex.core.definitions.annotations.{actionId, description}
import dev.vgerasimov.grafex.core.definitions.property._
import dev.vgerasimov.shapelse.annotations.AnnotatedShapeEncoder
import dev.vgerasimov.shapelse.names.NameShapeEncoder
import dev.vgerasimov.shapelse.{BooleanShape, ByteShape, CharShape, CompositeShape, CoproductShape, DoubleShape, FloatShape, IntShape, ListShape, LongShape, PrimitiveShape, ProductShape, Shape, ShapeEncoder, ShortShape, StringShape, combine, names, namesShapeEncoder, typenames, valueShapeEncoder, values}
//import dev.vgerasimov.scmc.{FieldEncoder, ProductField}

object implicits {

  object all extends FieldEncoders with PropertyMetadataEncoders

  trait FieldEncoders extends ComplexFieldEncoders

  trait ComplexFieldEncoders {

//    implicit def genericFieldEncoder[A, Repr](
//      implicit
//      typeTag: un.TypeTag[A], // FIXME: ???
//      gen: LabelledGeneric.Aux[A, Repr],
//      reprFieldEncoder: FieldEncoder[Repr],
//      metadataEncoder: Lazy[FieldsMetadataEncoder[A]] = null
//    ): FieldEncoder[A] = FieldEncoder.instance[A] {
//      reprFieldEncoder.encode match {
//        case field: ProductField =>
//          val metadata = metadataEncoder.value.encode
//          new AnnotatedObjectField(
//            field.childs.map({
//              case (s, p) => (s, (p, metadata.get(s)))
//            })
//          )
//          field // fixme ???
//        case x => x
//      }
//    }
  }

  trait PropertyMetadataEncoders {
    implicit def genericFieldMetadataEncoder[
      A,
      Repr <: HList,
      KeysRepr <: HList,
      DescRepr <: HList,
      ZipRepr <: HList
    ](
      implicit
      gen: LabelledGeneric.Aux[A, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      descriptions: Annotations.Aux[description, A, DescRepr],
      zip: Zip.Aux[KeysRepr :: DescRepr :: HNil, ZipRepr],
      toList: ToTraversable.Aux[ZipRepr, List, (Symbol, Option[description])]
    ): FieldsMetadataEncoder[A] = FieldsMetadataEncoder.instance {
      (keys() zip descriptions()).toList
        .map({
          case (symbol, maybeDescription) => (symbol, FieldMetadata(maybeDescription.map(_.s)))
        })
        .toMap
    }
  }


  import combine.implicits.tuples._
  import combine.syntax._
  import dev.vgerasimov.shapelse.empty.implicits.all._
  import dev.vgerasimov.shapelse.empty.instances._
  import names.implicits.all._
  import typenames.implicits.all._
  import values.implicits.all._

  implicit def kek[A](
                       implicit
                      nameFieldEncoder: NameShapeEncoder[A],
                      descFieldEncoder: AnnotatedShapeEncoder[description, A]
                     ) = {
    nameFieldEncoder.combine(descFieldEncoder).map({ case (n, d) => Meta(n, d.map(_.s)) })
//    nameFieldEncoder.map(Meta(_, None))
//    descFieldEncoder.map(d => Meta("", d.map(_.s)))
  }

  def deriveActionDefinition[A, Input, Output](implicit
                                            inShape: ShapeEncoder[Meta, Input],
                                            outShape: ShapeEncoder[Meta, Output],
                                               idAnn: Annotation[actionId, A],
                                               descAnn: Annotation[Option[description], A]
                                           ): action.Definition[A, Input, Output] =
    action.Definition(action.Id(idAnn.apply().name),
      InputSchema(foo[Input]), OutputSchema(foo[Output]), descAnn.apply().map(_.s))

  case class Meta(name: String, description: Option[String] = None)

  def foo[A](implicit shapeEncoder: ShapeEncoder[Meta, A]) = {
    val shape: Shape[Meta] = shapeEncoder.encode
    val definition: property.Definition = f(shape)
    definition
  }

  def f(shape: Shape[Meta]): property.Definition = {
    shape match {
      case shape: PrimitiveShape[_] => fp(shape.asInstanceOf[PrimitiveShape[Meta]])
      case shape: CompositeShape[_] => fc(shape.asInstanceOf[CompositeShape[Meta]])
    }
  }

  def fp(shape: PrimitiveShape[Meta]): property.Definition = {
    shape match {
      case BooleanShape(meta) => BooleanDefinition(meta.name, meta.description)
      case CharShape(meta)    => CharDefinition(meta.name, meta.description)
      case StringShape(meta)  => StringDefinition(meta.name, meta.description)
      case ByteShape(meta)    => ByteDefinition(meta.name, meta.description)
      case ShortShape(meta)   => ShortDefinition(meta.name, meta.description)
      case IntShape(meta)     => IntDefinition(meta.name, meta.description)
      case LongShape(meta)    => LongDefinition(meta.name, meta.description)
      case FloatShape(meta)   => FloatDefinition(meta.name, meta.description)
      case DoubleShape(meta)  => DoubleDefinition(meta.name, meta.description)
    }
  }

  def fc(shape: CompositeShape[Meta]): property.Definition = {
    shape match {
      case ListShape(meta, childs) => ListDefinition(meta.name, f(childs.head), meta.description)
      case ProductShape(meta, childs) =>
        ProductDefinition(meta.name, childs.map(s => (s.meta.name, f(s))).toMap, meta.description)
      case CoproductShape(meta, childs) =>
        CoproductDefinition(meta.name, childs.map(s => (s.meta.name, f(s))).toMap, meta.description)
    }
  }
}
