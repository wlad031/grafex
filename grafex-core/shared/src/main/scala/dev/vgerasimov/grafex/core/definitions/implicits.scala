package dev.vgerasimov.grafex
package core
package definitions

import shapeless.labelled.FieldType
import shapeless.ops.hlist.{ToTraversable, Zip}
import shapeless.ops.record.Keys
import shapeless.{:+:, ::, Annotations, CNil, Coproduct, HList, HNil, LabelledGeneric, Lazy, Witness}
import dev.vgerasimov.grafex.core.definitions.annotations.description
import dev.vgerasimov.grafex.core.definitions.property._
//import dev.vgerasimov.scmc.{FieldEncoder, ProductField}

import scala.reflect.runtime.{universe => un}

object implicits {

  object all extends FieldEncoders with PropertyMetadataEncoders

  trait FieldEncoders
      extends ComplexFieldEncoders


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
}
