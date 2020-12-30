package com.grafex

import cats.data.NonEmptyList
import com.grafex.core.{ InputType, Mode, ModeCallsParser, OutputType }

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

  /** Returns new [[Mode.Call]] with one [[Mode.Call]].
    * By default, uses Json as [[InputType]] and Json as [[OutputType]].
    */
  def createSingleModeRequest(
    call: Mode.Call,
    body: String,
    inputType: InputType = InputType.Json,
    outputType: OutputType = OutputType.Json
  ): Mode.Request = {
    Mode.Request(NonEmptyList(call, Nil), body, inputType, outputType)
  }
}
