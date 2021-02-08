package com.grafex

import cats.data.EitherT
import cats.effect.{ Clock, ExitCode, IO }
import com.grafex.core.errors.GrafexError
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
  def printWithExitCode(printStream: PrintStream)(message: => String)(exitCode: ExitCode): IO[ExitCode] = IO {
    printStream.println(message)
    exitCode
  }

  trait RunContext[F[_]] {
    val clock: Clock[F]
    val logger: Logger[F]
  }
}
