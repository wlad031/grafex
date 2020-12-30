package com.grafex

import cats.effect.{ Clock, ExitCode, IO }
import io.chrisdavenport.log4cats.Logger

import java.io.PrintStream

package object core {

  def printWithSuccess(message: String): IO[ExitCode] = printWithExitCode(System.out)(message)(ExitCode.Success)
  def printWithExitCode(printStream: PrintStream)(message: String)(exitCode: ExitCode): IO[ExitCode] =
    IO {
      printStream.println(message)
      exitCode
    }

  trait GrafexError extends RuntimeException { self =>

    override def getMessage: String = self.toString

    def errorIO: IO[ExitCode] = IO {
      System.err.println(getMessage)
      ExitCode.Error
    }
  }

  sealed trait ArgsFastExit extends GrafexError

  case class ArgsParsingError(help: String) extends ArgsFastExit {
    override def errorIO: IO[ExitCode] = IO {
      System.err.println(help)
      ExitCode.Error
    }
  }

  case class HelpRequest(help: String) extends ArgsFastExit {
    def successIO: IO[ExitCode] = IO {
      System.out.println(help)
      ExitCode.Success
    }
  }

  case class VersionRequest(version: String) extends ArgsFastExit {
    def successIO: IO[ExitCode] = IO {
      System.out.println(version)
      ExitCode.Success
    }
  }

  trait ModeError extends GrafexError
  final case class UnknownAction(actionKey: Mode.Action.Key) extends ModeError
  final case class UnsupportedInputType(inputType: InputType) extends ModeError
  final case class UnsupportedOutputType(outputType: OutputType) extends ModeError
  final case class IllegalModeState() extends ModeError // FIXME: bad error
  final case class RequestFormatError(request: Mode.SingleCallRequest, ex: Exception) extends ModeError
  final case class ResponseFormatError(response: Mode.Response, ex: Exception) extends ModeError

  trait RunContext[F[_]] {
    val clock: Clock[F]
    val logger: Logger[F]
  }

  sealed trait InputType {
    override def toString: String =
      this match {
        case InputType.Json => "json"
      }
  }

  /** Factory for [[InputType]].
    * Also, contains all the implementations of [[InputType]].
    */
  object InputType {
    def fromString(s: String): Option[InputType] =
      s match {
        case "json" => Some(Json)
        case _      => None
      }

    case object Json extends InputType
  }

  sealed trait OutputType {
    override def toString: String =
      this match {
        case OutputType.Json       => "json"
        case OutputType.PlainText  => "plain"
        case OutputType.PrettyText => "pretty"
      }
  }

  /** Factory for [[OutputType]].
    * Also, contains all the implementations of [[OutputType]].
    */
  object OutputType {
    def fromString(s: String): Option[OutputType] =
      s match {
        case "json"   => Some(Json)
        case "plain"  => Some(PlainText)
        case "pretty" => Some(PrettyText)
        case _        => None
      }

    case object Json extends OutputType
    case object PlainText extends OutputType
    case object PrettyText extends OutputType
  }
}
