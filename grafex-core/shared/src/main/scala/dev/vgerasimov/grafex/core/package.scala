package dev.vgerasimov.grafex

import cats.{Monad, Semigroup}
import cats.data.{EitherT, NonEmptyList}
import cats.effect.{Clock, ExitCode, IO}
import dev.vgerasimov.grafex.core.errors.GrafexError
import io.chrisdavenport.log4cats.Logger

import cats.implicits._

import java.io.PrintStream

package object core {

  sealed trait PathItem
  object PathItem {
    final case class Id(s: String) extends PathItem
    final case class Binding[A](name: String, value: A) extends PathItem
  }

  type Path = NonEmptyList[PathItem]
  type Paths = NonEmptyList[Path]

  type RequestPath = String
  type Router[F[_], A, B] = PartialFunction[RequestPath, EitherE[Act[F, A, B]]]
  type Act[F[_], A, B] = A => EitherET[F, B]
  type Converter[A, B] = A => EitherE[B]

  def hello[F[_] : Monad]: Act[F, String, String] = name => EitherT.rightT(s"hello, $name")

  def router[F[_] : Monad]: Router[F, String, String] = {
    case "/foo/bar" => hello[F].asRight
  }

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
