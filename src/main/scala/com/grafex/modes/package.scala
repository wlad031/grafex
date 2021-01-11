package com.grafex

import cats.data.NonEmptyList
import com.grafex.core.modeFoo.{ Mode, ModeRequest }
import com.grafex.core.{ ModeCallsParser, OutputType }
import io.circe.syntax.EncoderOps

package object modes {

  /** Runs parsing of single mode call and unsafely returns the result.
    *
    * @throws RuntimeException in case if parsing error happened
    *
    * @todo Make it package private.
    *
    * @note Should not be used in dynamic context as it's not safe.
    */
  def unsafeParseSingleModeCall(s: String): Mode.Call = ModeCallsParser.parse(s) match {
    case Left(error)  => sys.error(s"Unexpected error $error")
    case Right(value) => value.head
  }

  object ModeClient {

    def jsonRequest[A : io.circe.Encoder](
      call: Mode.Call,
      body: A
    ): ModeRequest = ModeRequest.Json(NonEmptyList(call, Nil), OutputType.Json, body.asJson)
  }
}
