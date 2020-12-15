package com.grafex.core.boot

import java.nio.file.Path

import cats.data.NonEmptyList
import com.grafex.core._

/** ADT which describes the "startup mode" of the application.
  *
  * For now supported the following ways to start the application:
  *   1. when `--version` flag specified, the application version will be printed to STDOUT
  *      and then application will exit with code = 0;
  *   2. when `--help` flag specified, the help message will be printed to STDOUT
  *      and then application will exit with code = 0;
  *   3. when at least one mode call specified, application will process it, print the result to STDOUT
  *      and exit with code = 0, or print the error to STDERR and exit with code = 1;
  *   4. when `service` subcommand and at least one listener specified, application will be processing incoming
  *      calls until user or system kill/exit it or some severe error happen.
  */
sealed trait Startup

/** Contains implementations of [[Startup]]. */
object Startup {
  case class Version(version: String) extends Startup
  case class Help() extends Startup

  sealed trait Context extends Startup {
    val configPaths: List[Path]
    val verbosity: Verbosity
  }

  object Context {

    case class Service(
      override val configPaths: List[Path],
      override val verbosity: Verbosity,
      listeners: NonEmptyList[Listener]
    ) extends Context

    case class Cli(
      override val configPaths: List[Path],
      override val verbosity: Verbosity,
      calls: NonEmptyList[Mode.Call],
      params: Cli.Data,
      inputType: InputType,
      outputType: OutputType
    ) extends Context

    object Cli {
      sealed trait Data
      object Data {
        case class Json(json: String) extends Data
      }
    }
  }

  sealed trait Listener
  object Listener {
    case class Web() extends Listener
    case class Socket() extends Listener
  }

  sealed trait Verbosity {
    def asLoggerName: String = this match {
      case Verbosity.Normal()  => "grafex"
      case Verbosity.Verbose() => "grafex-v"
      case Verbosity.Debug()   => "grafex-debug"
    }
  }

  object Verbosity {
    case class Normal() extends Verbosity
    case class Verbose() extends Verbosity
    case class Debug() extends Verbosity
  }
}
