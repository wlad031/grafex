package com.grafex

import cats.data.EitherT
import cats.effect.{ Clock, ExitCode, IO }
import io.chrisdavenport.log4cats.Logger

import java.io.PrintStream

package object core {

  type EitherE[+A] = Either[GrafexError, A]
  type EitherET[F[_], A] = EitherT[F, GrafexError, A]

  def unsafe[A, B](either: => Either[A, B]): B = either match {
    case Left(error)  => sys.error(s"Unexpected error: $error")
    case Right(value) => value
  }

  def printWithSuccess(message: => String): IO[ExitCode] = printWithExitCode(System.out)(message)(ExitCode.Success)
  def printWithError(message: => String): IO[ExitCode] = printWithExitCode(System.err)(message)(ExitCode.Error)
  def printWithExitCode(printStream: PrintStream)(message: => String)(exitCode: ExitCode): IO[ExitCode] =
    IO {
      printStream.println(message)
      exitCode
    }

  trait GrafexError extends RuntimeException { self =>

    private[this] def msg: String = self.toString

    def errorIO: IO[ExitCode] = IO {
      System.err.println(msg)
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

  trait RunContext[F[_]] {
    val clock: Clock[F]
    val logger: Logger[F]
  }
}
