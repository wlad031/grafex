package com.grafex.core
package boot

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }
import cats.syntax.all._
import com.grafex.build.BuildInfo
import com.grafex.core.boot.Startup.{ Listener, Verbosity }
import com.monovore.decline.{ Command, Opts }

import java.nio.file.Path

/** Contains functionality for parsing command line arguments and environment variables.
  *
  */
object ArgsParser {

  /** Parses the list of arguments and returns either [[ArgsFastExit]] in case of parsing failed
    * or [[Startup.Context]] otherwise.
    */
  def parse(args: List[String]): Either[ArgsFastExit, Startup.Context] = {
    command.parse(args, sys.env) match {
      case Left(help)                      => Left(ArgsParsingError(help.toString()))
      case Right(_: Startup.Help)          => Left(HelpRequest(command.showHelp))
      case Right(Startup.Version(version)) => Left(VersionRequest(version))
      case Right(ctx: Startup.Context)     => Right(ctx)
    }
  }

  private val command: Command[Startup] = {

    val verboseFlag = Opts.flag(long = "verbose", help = "Verbose output").orFalse
    val debugFlag = Opts.flag(long = "debug", help = "Debug output").orFalse
    val verbosityOpt = (verboseFlag, debugFlag).mapN((isVerbose, isDebug) =>
      (isVerbose, isDebug) match {
        case (false, false) => Verbosity.Normal
        case (true, false)  => Verbosity.Verbose
        case (_, true)      => Verbosity.Debug
      }
    )

    val userHomeOpt = Opts
      .env[Path]("HOME", help = "User home directory")

    val configOpt = Opts
      .options[Path](short = "c", long = "config", help = "Paths to configuration files")
      .orEmpty

    val versionOpt = Opts
      .flag(long = "version", help = "Print the version number and exit")
      .map(_ => BuildInfo.version)
      .map(Startup.Version)

    // Using explicitly created "help" allows to
    // distinguish scenario when user used "--help" argument
    // from scenarios when user typed arguments
    // which cannot be parsed. Otherwise, "decline" returns
    // help message in both cases (to be honest, with
    // filled or not "errors" field, but sometimes
    // it is not enough).
    val helpOpt = Opts
      .flag(long = "help", help = "Print this help and exit")
      .map(_ => Startup.Help())

    val serviceSubCommand = Opts
      .subcommand("service", "Run application as a service which can listen to different kinds of events")(
        (
          Opts.flag(long = "web", help = "Listen to web requests").orFalse,
          Opts.flag(long = "socket", help = "Listen to raw socket requests").orFalse
        ).tupled
      )
      .mapValidated({
        case (true, true)  => NonEmptyList(Listener.Web(), List(Listener.Socket())).valid
        case (true, false) => NonEmptyList(Listener.Web(), Nil).valid
        case (false, true) => NonEmptyList(Listener.Socket(), Nil).valid
        case (false, false) =>
          s"""Invalid service run, please specify at least one of the listeners:
             |  --web
             |  --socket""".stripMargin.invalidNel
      })

    val callsOpt = Opts
      .argument[String]("calls")
      .mapValidated(calls => {
        ModeCallsParser.parse(calls) match {
          case Left(error) =>
            s"""Error parsing calls: $calls - $error
               |Please use the following pattern: <mode>.<version>/<action>""".stripMargin.invalidNel
          case Right(value) => value.valid
        }
      })

    val optionsOpt = Opts
      .option[String](long = "options", short = "o", help = "Options to be passed along with the data")
      .mapValidated(options => {
        ModeRequestOptionsParser.parse(options) match {
          case Left(error) =>
            s"""Error parsing options: $options - $error""
               |Please use the following pattern: <key1>=<value1>[;<key2=value2>]...""".stripMargin.invalidNel
          case Right(value) => value.valid
        }
      })
      .orNone
      .map({
        case Some(options) => options
        case None          => Map[String, String]()
      })

    val dataOpt = Opts.arguments[String]("data").orEmpty

    val serviceOpt = (
      userHomeOpt,
      configOpt,
      verbosityOpt,
      serviceSubCommand
    ).mapN(Startup.Context.Service.apply)

    val normalOpt = (
      userHomeOpt,
      configOpt,
      verbosityOpt,
      callsOpt,
      dataOpt,
      optionsOpt
    ).mapN(Startup.Context.Cli.apply)

    Command(BuildInfo.name, "Grafex", helpFlag = false)(
      List(
        versionOpt,
        helpOpt,
        serviceOpt,
        normalOpt
      ).reduce(_ orElse _)
    )
  }
}
