package com.grafex.core
package boot

import cats.data.NonEmptyList

import java.nio.file.Path

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

  /** "Startup mode" if application started with `--version` argument. */
  final case class Version(version: String) extends Startup

  /** "Startup mode" if application started with `--help` argument. */
  final case class Help() extends Startup

  /** Represents the startup context if application started in some of normal modes. */
  sealed trait Context extends Startup {

    /** Path to the home directory. */
    def userHome: Path

    /** Additional configuration file paths. */
    def configPaths: List[Path]

    /** Represents how much information application will be printing during it's running time. */
    def verbosity: Verbosity
  }

  /** Contains implementations of [[Context]]. */
  object Context {

    final case class Service(
      override val userHome: Path,
      override val configPaths: List[Path],
      override val verbosity: Verbosity,
      listeners: NonEmptyList[Listener]
    ) extends Context

    final case class Cli(
      override val userHome: Path,
      override val configPaths: List[Path],
      override val verbosity: Verbosity,
      calls: NonEmptyList[Mode.Call],
      data: List[String],
      options: Map[String, String]
    ) extends Context
  }

  sealed trait Listener
  object Listener {
    case class Web() extends Listener
    case class Socket() extends Listener
  }

  sealed trait Verbosity {
    protected def level: Int

    def > (that: Verbosity): Boolean = this.level > that.level
    def >= (that: Verbosity): Boolean = this.level >= that.level

    def asLoggerName: String = this match {
      case Verbosity.Normal  => "grafex"
      case Verbosity.Verbose => "grafex-v"
      case Verbosity.Debug   => "grafex-d"
    }
  }

  object Verbosity {
    final case object Normal extends Verbosity { override val level: Int = 1 }
    final case object Verbose extends Verbosity { override val level: Int = 2 }
    final case object Debug extends Verbosity { override val level: Int = 3 }
  }
}
