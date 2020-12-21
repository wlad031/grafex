package com.grafex.core

import cats.data.NonEmptyList
import cats.syntax.either._
object ModeCallsParser {

  sealed trait ParseError extends GrafexError
  object ParseError {
    final case class MissingAction(call: String) extends ParseError
    final case class InvalidCall(call: String) extends ParseError
    final case class EmptyCall() extends ParseError
    final case class EmptySubCall(call: String) extends ParseError
  }

  def parse(s: String): Either[ParseError, NonEmptyList[Mode.Call]] = s match {
    case "" => Left(ParseError.EmptyCall())
    case calls =>
      calls
        .split(">")
        .toList
        .map({
          case "" => Left(ParseError.EmptySubCall(calls))
          case x =>
            x.split("/", 2).toList match {
              case _ :: Nil => Left(ParseError.MissingAction(x))
              case mode :: action :: Nil =>
                mode.split("""\.""", 2).toList match {
                  case modeName :: version :: Nil => Right(createFullCall(modeName, version, action))
                  case modeName :: Nil            => Right(createLatestCall(modeName, action))
                  case _                          => Left(ParseError.InvalidCall(calls))
                }
              case _ => Left(ParseError.InvalidCall(x))
            }
        })
        .reduce(_ combine _)
  }

  private def createFullCall(modeName: String, version: String, actionName: String): NonEmptyList[Mode.Call] = {
    NonEmptyList(
      Mode.Call.full(
        Mode.Key(Mode.Name(modeName), Mode.Version(version)),
        Mode.Action.Key(Mode.Action.Name(actionName))
      ),
      Nil
    )
  }

  private def createLatestCall(modeName: String, actionName: String): NonEmptyList[Mode.Call] = {
    NonEmptyList(
      Mode.Call.latest(
        Mode.Name(modeName),
        Mode.Action.Key(Mode.Action.Name(actionName))
      ),
      Nil
    )
  }
}
