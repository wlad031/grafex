package com.grafex.core

import cats.data.NonEmptyList
import cats.syntax.option._
import com.grafex.core.definitions._

sealed trait ModeRequest {
  def calls: NonEmptyList[Mode.Call]
  def inputType: InputType
  def outputType: OutputType

  def firstCall: Mode.Call = calls.head
  def dropTail(outputType: OutputType): ModeRequest
  def dropFirst(): Option[ModeRequest]
}

object ModeRequest {

  def web(call: Mode.Call, body: String) = {
    Json(NonEmptyList(call, Nil), OutputType.Json, io.circe.parser.parse(body).getOrElse(???)) // FIXME
  }

  final case class Json(
    override val calls: NonEmptyList[Mode.Call],
    override val outputType: OutputType,
    body: io.circe.Json
  ) extends ModeRequest {

    override val inputType: InputType = InputType.Json

    override def dropTail(newOutputType: OutputType = outputType): ModeRequest = {
      copy(calls = NonEmptyList(this.calls.head, Nil), outputType = newOutputType)
    }

    override def dropFirst(): Option[ModeRequest] = this.calls match {
      case NonEmptyList(_, Nil)     => None
      case NonEmptyList(_, x :: xs) => copy(calls = NonEmptyList(x, xs)).some
    }
  }

}

sealed trait ModeResponse {
  def toString: String
}

object ModeResponse {

  final case class Json(body: io.circe.Json) extends ModeResponse {
    override def toString: String = body.spaces2
  }

}

trait ModeError extends GrafexError

object ModeError {
  final case class UnknownAction(actionId: action.Id) extends ModeError
  final case class RequestFormatError(request: ModeRequest, ex: Exception) extends ModeError
  final case class ResponseFormatError(response: ModeResponse, ex: Exception) extends ModeError

  sealed trait InvalidRequest extends ModeError
  object InvalidRequest {
    final case class UnsupportedInputType(request: ModeRequest) extends InvalidRequest
    final case class UnsupportedOutputType(outputType: OutputType) extends InvalidRequest

    final case class ModesNotCombinable(first: mode.Definition, second: mode.Definition) extends InvalidRequest

    final case class WrongMode(modeDefinition: mode.Definition, request: ModeRequest) extends InvalidRequest
    final case class NotEnoughCalls(modeDefinition: definitions.mode.Definition, request: ModeRequest)
        extends InvalidRequest
  }
}
