package com.grafex
package core
package boot

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }
import cats.syntax.all._
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
        case (false, false) => Verbosity.Normal()
        case (true, false)  => Verbosity.Verbose()
        case (_, true)      => Verbosity.Debug()
      }
    )

    val userHomeOpt = Opts
      .env[Path]("HOME", help = "User home directory")

    val configOpt = Opts
      .options[Path](short = "c", long = "config", help = "Paths to configuration files")
      .orEmpty

    val versionOpt = Opts
      .flag(long = "version", help = "Print the version number and exit")
      .map(_ => build.BuildInfo.version)
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
        case (true, true)  => Valid(NonEmptyList(Listener.Web(), List(Listener.Socket())))
        case (true, false) => Valid(NonEmptyList(Listener.Web(), Nil))
        case (false, true) => Valid(NonEmptyList(Listener.Socket(), Nil))
        case (false, false) =>
          Invalid(
            NonEmptyList(
              s"""Invalid service run, please specify at least one of the listeners:
                 |  --web
                 |  --socket""".stripMargin,
              Nil
            )
          )
      })

    val callsOpt = Opts
      .argument[String]("calls")
      .mapValidated(calls => {
        calls
          .split(">")
          .toList
          .map(call =>
            call.split("/", 2).toList match {
              case mode :: action :: Nil =>
                mode.split("""\.""", 2).toList match {
                  case modeName :: version :: Nil =>
                    Right(
                      Mode.Call.Full(
                        Mode.Key(Mode.Name(modeName), Mode.Version(version)),
                        Mode.Action.Key(Mode.Action.Name(action))
                      )
                    )
                  case modeName :: Nil =>
                    Right(
                      Mode.Call.Latest(
                        Mode.Name(modeName),
                        Mode.Action.Key(Mode.Action.Name(action))
                      )
                    )
                  case _ => ???
                }
              case _ =>
                Left(
                  s"""Invalid mode call: $call
                   |Please use the following pattern: <mode>/<version>/<action>""".stripMargin
                )
            }
          )
          .map(x => x.map(NonEmptyList(_, Nil)))
          .reduce((e1, e2) => {
            e1 match {
              case x @ Left(_) => x
              case Right(l) =>
                e2 match {
                  case y @ Left(_) => y
                  case Right(r)    => Right(l ::: r)
                }
            }
          }) match {
          case Left(e)  => Invalid(NonEmptyList(e, Nil))
          case Right(v) => Valid(v)
        }
      })

    val inputTypeOpt = Opts
      .option[String](long = "in", help = "input type")
      .mapValidated({
        case "json" => Valid(InputType.Json)
        case x =>
          Invalid(
            NonEmptyList(
              s"""Unknown input type: $x.
                 |Available input types: json.
                 |""".stripMargin,
              Nil
            )
          )
      })
      .orNone
      .map({
        case Some(inputType) => inputType
        case None            => InputType.Json // default input type
      })

    val outputTypeOpt = Opts
      .option[String](long = "out", help = "output type")
      .mapValidated({
        case "json"   => Valid(OutputType.Json)
        case "pretty" => Valid(OutputType.PrettyText)
        case "plain"  => Valid(OutputType.PlainText)
        case x =>
          Invalid(
            NonEmptyList(
              s"""Unknown output type: $x.
                 |Available output types: json / pretty / plain.""".stripMargin,
              Nil
            )
          )
      })
      .orNone
      .map({
        case Some(outputType) => outputType
        case None             => OutputType.Json // default output type
      })

    val dataOpt = Opts.arguments[String]("data").orEmpty

    val serviceOpt = (
      userHomeOpt,
      configOpt,
      verbosityOpt,
      serviceSubCommand
    ).mapN((userHome, configPaths, verbosity, listeners) => {
      Startup.Context.Service(userHome, configPaths, verbosity, listeners)
    })

    val normalOpt = (
      userHomeOpt,
      configOpt,
      verbosityOpt,
      callsOpt,
      inputTypeOpt,
      outputTypeOpt,
      dataOpt
    ).mapN((userHome, configPaths, verbosity, call, inputType, outputType, params) => {
      // TODO: this should be refactored
      Startup.Context.Cli(
        userHome,
        configPaths,
        verbosity,
        call,
        inputType match {
          case InputType.Json =>
            Startup.Context.Cli.Data.Json(params match {
              case Nil    => "{}"
              case x :: _ => x
            })
        },
        inputType,
        outputType
      )
    })

    Command(build.BuildInfo.name, "Grafex", helpFlag = false)(
      List(
        versionOpt,
        helpOpt,
        serviceOpt,
        normalOpt
      ).reduce(_ orElse _)
    )
  }
}
