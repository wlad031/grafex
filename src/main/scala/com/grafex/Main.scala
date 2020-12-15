package com.grafex

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{Clock, ExitCode, IO, IOApp}
import com.grafex.core.boot.Config.GrafexConfig
import com.grafex.core.boot.Startup.Listener
import com.grafex.core.boot.{ArgsParser, Config, Startup}
import com.grafex.core.implicits._
import com.grafex.core.listeners.{SocketListener, WebListener}
import com.grafex.core.{ArgsParsingError, VersionRequest, _}
import com.grafex.modes.datasource.DataSourceMode
import com.grafex.modes.describe.DescribeMode
import com.grafex.modes.describe.DescribeMode._
import com.grafex.modes.account.AccountMode
import com.grafex.modes.account.AccountMode._
import com.grafex.modes.graph.GraphMode
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  /** Grafex application main entrypoint.
    *
    * @param args the list of command line arguments
    * @return exit code wrapped in [[IO]]
    */
  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      startupCtx <- ArgsParser.parse(args)
      config     <- Config.read(startupCtx)
      runCtx     <- buildRunContext(startupCtx)
      mode       <- buildModeContainer(startupCtx, runCtx, config)
      exitCode   <- launch(startupCtx, runCtx, mode)
    } yield exitCode).value.flatMap({
      case Left(argsHelp: ArgsParsingError) => argsHelp.errorIO
      case Left(help: HelpRequest)          => help.successIO
      case Left(version: VersionRequest)    => version.successIO
      case Left(error: GrafexError)         => error.errorIO
      case Right(exitCode)                  => IO.pure(exitCode)
    })
  }

  def buildRunContext(startupContext: Startup.Context): EitherT[IO, GrafexError, RunContext[IO]] =
    EitherT.right(for {
      l <- startupContext match {
        case Startup.Context.Service(_, _, _)              => Slf4jLogger.fromName[IO]("grafex-service")
        case Startup.Context.Cli(_, verbosity, _, _, _, _) => Slf4jLogger.fromName[IO](verbosity.asLoggerName)
      }
    } yield new RunContext[IO] {
      override val clock: Clock[IO] = Clock.create[IO]
      override val logger: Logger[IO] = l
    })

  def buildModeContainer(
    startupCtx: Startup.Context,
    runCtx: RunContext[IO],
    config: GrafexConfig
  ): EitherT[IO, GrafexError, Mode[IO]] = {
    implicit val rCtx: RunContext[IO] = runCtx

    val metaDataSource = config.metaDataSource match {
      case x: GrafexConfig.Foo => new Neo4jMetaDataSource(x)
    }

    val modes: List[Mode[IO]] = List(
      Mode.instance(DataSourceMode.definition.toLatest)(
        new DataSourceMode(metaDataSource)),
      Mode.instance(GraphMode.definition.toLatest)(new GraphMode(metaDataSource)),
      Mode.instance(AccountMode.definition.toLatest)(new AccountMode(null))
    )

    val describeMode: Mode[IO] = Mode.instance(DescribeMode.definition.toLatest)(
      new DescribeMode(
        modes
          .map(_.definition)
          .map(_.asInstanceOf[Mode.Definition.Basic])
      )
    )

    EitherT.rightT(modes.foldLeft(describeMode)(_ orElse _))
  }

  def buildCliRequest(
    calls: NonEmptyList[Mode.Call],
    data: Startup.Context.Cli.Data,
    inputType: InputType,
    outputType: OutputType
  ): Either[GrafexError, Mode.Request] = data match {
    case Startup.Context.Cli.Data.Json(json) => Right(Mode.Request(calls, json, inputType, outputType))
  }

  def launch(
    startupCtx: Startup.Context,
    runCtx: RunContext[IO],
    modeContainer: Mode[IO]
  ): EitherT[IO, GrafexError, ExitCode] = startupCtx match {

    case Startup.Context.Cli(_, _, calls, data, inputType, outputType) =>
      for {
        req      <- EitherT.fromEither[IO](buildCliRequest(calls, data, inputType, outputType))
        res      <- modeContainer(req)
        exitCode <- EitherT.right(printWithSuccess(res.body))
      } yield exitCode

    case Startup.Context.Service(_, _, listeners) =>
      listeners
        .map({
          case Listener.Web()    => WebListener(startupCtx, modeContainer)
          case Listener.Socket() => SocketListener(startupCtx, modeContainer)
        })
        .toList
        .reduce(_ combine _)
  }
}
