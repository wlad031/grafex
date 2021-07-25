package dev.vgerasimov.grafex

import cats.data.NonEmptyList
import dev.vgerasimov.grafex.core.{ Mode, ModeCallsParser, ModeRequest, unsafe }
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
  def unsafeParseSingleModeCall(s: String): Mode.Call = unsafe(ModeCallsParser.parse(s)).head

  object ModeClient {

    def jsonRequest[A : io.circe.Encoder](
      call: Mode.Call,
      body: A
    ): ModeRequest = ModeRequest(NonEmptyList(call, Nil), List(body.asJson.noSpaces), options = Map("input" -> "json"))
  }
}
