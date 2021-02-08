package com.grafex.core

import cats.effect.{ ExitCode, IO }
import com.grafex.core.definitions.{ action, mode }

object errors {

  // TODO: make it sealed
  trait GrafexError extends RuntimeException { self =>
    private[this] def msg: String = self.toString

    def errorIO: IO[ExitCode] = IO {
      System.err.println(msg)
      ExitCode.Error
    }
  }

  sealed trait ArgsFastExit extends GrafexError

  final case class ArgsParsingError(help: String) extends ArgsFastExit {
    override def errorIO: IO[ExitCode] = IO {
      System.err.println(help)
      ExitCode.Error
    }
  }

  final case class HelpRequest(help: String) extends ArgsFastExit {
    def successIO: IO[ExitCode] = IO {
      System.out.println(help)
      ExitCode.Success
    }
  }

  final case class VersionRequest(version: String) extends ArgsFastExit {
    def successIO: IO[ExitCode] = IO {
      System.out.println(version)
      ExitCode.Success
    }
  }

  final case class UnknownAction(actionId: action.Id) extends GrafexError
  final case class ResponseFormatError(modeResponse: ModeResponse, throwable: Throwable) extends GrafexError

  sealed trait InvalidRequest extends GrafexError
  object InvalidRequest {
    final case class UnsupportedEmptyRequest(request: ModeRequest) extends InvalidRequest
    final case class UnsupportedOption(key: String, value: String) extends InvalidRequest
    final case class InvalidJson(throwable: Throwable) extends InvalidRequest
    final case class InvalidTypeOfJson(throwable: Throwable) extends InvalidRequest
    final case class UnsupportedAction(s: String) extends InvalidRequest

    final case class ModesNotCombinable(first: mode.Definition, second: mode.Definition) extends InvalidRequest

    final case class WrongMode(modeDefinition: mode.Definition, request: ModeRequest) extends InvalidRequest
    final case class NotEnoughCalls(modeDefinition: definitions.mode.Definition, request: ModeRequest)
        extends InvalidRequest
  }

}
