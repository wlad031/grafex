package dev.vgerasimov.grafex
package core
package conversion

import shapeless.Lazy
import cats.syntax.either._

trait JsonActionResponseEncoder[A] extends ActionResponseEncoder[A]

object JsonActionResponseEncoder {
  def instance[A](
    f: (A, String) => ModeResponse
  )(implicit jsonEncoder: Lazy[io.circe.Encoder[A]]): JsonActionResponseEncoder[A] =
    new JsonActionResponseEncoder[A] {

      override def isDefinedAt(x: (ModeRequest, A)): Boolean = x match {
        case (ModeRequest(_, _, options), _) =>
          Set("json").contains(options.getOrElse("format", null)) ||
          Set("application/json").contains(options.getOrElse("Accept", null))
      }

      override def apply(x: (ModeRequest, A)): EitherE[ModeResponse] = {
        f(x._2, jsonEncoder.value.apply(x._2).spaces2).asRight
      }
    }
}
