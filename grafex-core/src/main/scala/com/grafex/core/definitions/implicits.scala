package com.grafex.core
package definitions

import shapeless.labelled.FieldType
import shapeless.ops.hlist.{ ToTraversable, Zip }
import shapeless.ops.record.Keys
import shapeless.{ :+:, ::, Annotations, CNil, Coproduct, HList, HNil, LabelledGeneric, Lazy, Witness }
import com.grafex.core.definitions.annotations.description
import com.grafex.core.definitions.property._

import scala.reflect.runtime.{ universe => un }

object implicits {

  object all extends FieldEncoders with PropertyMetadataEncoders

  trait FieldEncoders
      extends PrimitiveFieldEncoders
      with ComplexFieldEncoders
      with HListFieldEncoders
      with CoproductFieldEncoders

  trait PrimitiveFieldEncoders {
    implicit val stringFieldEncoder: FieldEncoder[String] = FieldEncoder.instance(StringField)
    implicit val booleanFieldEncoder: FieldEncoder[Boolean] = FieldEncoder.instance(BooleanField)
    implicit val intFieldEncoder: FieldEncoder[Int] = FieldEncoder.instance(IntField)
    implicit val floatFieldEncoder: FieldEncoder[Float] = FieldEncoder.instance(FloatField)
  }

  trait ComplexFieldEncoders {

    implicit def optionFieldEncoder[A](implicit ev: FieldEncoder[A]): FieldEncoder[Option[A]] =
      FieldEncoder.instance(OptionField(ev.encode))

    implicit def listFieldEncoder[A](implicit ev: FieldEncoder[A]): FieldEncoder[List[A]] =
      FieldEncoder.instance(ListField(ev.encode))

    implicit def genericFieldEncoder[A, Repr](
      implicit
      typeTag: un.TypeTag[A],
      gen: LabelledGeneric.Aux[A, Repr],
      reprFieldEncoder: FieldEncoder[Repr],
      metadataEncoder: Lazy[FieldsMetadataEncoder[A]] = null
    ): FieldEncoder[A] = FieldEncoder.instance[A] {
      reprFieldEncoder.encode match {
        case field: ObjectField =>
          val metadata = metadataEncoder.value.encode
          new AnnotatedObjectField(
            field.fields.map({
              case (s, p) => (s, (p, metadata.get(s)))
            })
          )
        case x => x
      }
    }
  }

  trait HListFieldEncoders {
    implicit val hnilFieldEncoder: ObjectFieldEncoder[HNil] =
      ObjectFieldEncoder.instance[HNil](ObjectField(Map()))

    implicit def hlistFieldEncoder[K <: Symbol, H, T <: HList](
      implicit
      witness: Witness.Aux[K],
      hEncoder: Lazy[FieldEncoder[H]],
      tEncoder: ObjectFieldEncoder[T]
    ): ObjectFieldEncoder[FieldType[K, H] :: T] = ObjectFieldEncoder.instance {
      ObjectField(tEncoder.encode.fields + (witness.value -> hEncoder.value.encode))
    }
  }

  trait CoproductFieldEncoders {
    implicit val cnilFieldEncoder: FieldEncoder[CNil] = FieldEncoder.instance(
      // FIXME: should be sys.error("Unreachable code")
      //        but for some reason this code evaluates and, of course, fails
      BooleanField
    )

    implicit def coproductFieldEncoder[K <: Symbol, H, T <: Coproduct](
      implicit
      witness: Witness.Aux[K],
      hEncoder: Lazy[FieldEncoder[H]],
      tEncoder: FieldEncoder[T]
    ): FieldEncoder[FieldType[K, H] :+: T] = FieldEncoder.instance[FieldType[K, H] :+: T] {
      EitherField(hEncoder.value.encode, tEncoder.encode)
    }
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
