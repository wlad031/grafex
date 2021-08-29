package dev.vgerasimov.grafex
package core

import cats.data.NonEmptyList
import cats.syntax.option._

final case class ModeRequest(
  calls: NonEmptyList[Mode.Call],
  body: List[String],
  options: Map[String, String] = Map()
) {
  def firstCall: Mode.Call = this.calls.head
  def dropTail: ModeRequest = copy(calls = NonEmptyList(this.calls.head, Nil))

  def dropFirst(): Option[ModeRequest] = this.calls match {
    case NonEmptyList(_, Nil)     => None
    case NonEmptyList(_, x :: xs) => copy(calls = NonEmptyList(x, xs)).some
  }
}

sealed trait ErrorCode
object ErrorCode {
  final case object ClientError extends ErrorCode
  final case object ServerError extends ErrorCode
}

abstract class ErrorMeta(val code: ErrorCode)

sealed trait ModeResponse

object ModeResponse {

  final case class Ok(
    body: String,
    options: Map[String, String] = Map()
  ) extends ModeResponse

  final case class Error(
    body: String,
    errorCode: ErrorCode,
    options: Map[String, String] = Map()
  ) extends ModeResponse
}
