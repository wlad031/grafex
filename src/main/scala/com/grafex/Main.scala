package com.grafex

import cats.data.EitherT
import cats.effect.{ Clock, ExitCode, IO, IOApp, Resource }
import cats.syntax.either._
import com.grafex.core.boot.Config.GrafexConfiguration
import com.grafex.core.boot.Startup.{ Listener, Verbosity }
import com.grafex.core.boot.{ ArgsParser, Config, Startup }
import com.grafex.core.definitions.mode
import com.grafex.core.errors.{ ArgsParsingError, GrafexError, HelpRequest, VersionRequest }
import com.grafex.core.graph.GraphDataSource
import com.grafex.core.graph.neo4j.Neo4JGraphDataSource
import com.grafex.core.implicits._
import com.grafex.core.internal.neo4j.{ logging => Neo4JLogging }
import com.grafex.core.listeners.{ SocketListener, WebListener }
import com.grafex.core._
import com.grafex.modes.account.AccountMode
import com.grafex.modes.datasource.DataSourceMode
import com.grafex.modes.describe.DescribeMode
import com.grafex.modes.graph.GraphMode
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import neotypes.GraphDatabase
import neotypes.cats.effect.implicits._
import org.neo4j.driver.{ AuthTokens, Config => Neo4JConfig }

object Main extends IOApp {

  /** Grafex application main entrypoint.
    *
    * @param args the list of command line arguments
    * @return exit code wrapped in [[cats.effect.IO]]
    */
  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      startupCtx <- ArgsParser.parse(args).toEitherT[IO]
      config     <- Config.load(startupCtx)()
      runCtx     <- buildRunContext(startupCtx)
      exitCode <- EitherT[IO, GrafexError, ExitCode](buildModeContainer(startupCtx, runCtx, config).use {
        case Left(error) => IO.pure(error.asLeft)
        case Right(mode) => launch(startupCtx, runCtx, mode).value
      })
    } yield exitCode).value.flatMap({
      case Left(argsHelp: ArgsParsingError) => argsHelp.errorIO
      case Left(help: HelpRequest)          => help.successIO
      case Left(version: VersionRequest)    => version.successIO
      case Left(error: GrafexError)         => error.errorIO
      case Right(exitCode)                  => IO.pure(exitCode)
    })
  }

  def buildRunContext(startupContext: Startup.Context): EitherT[IO, GrafexError, RunContext[IO]] = EitherT.right {
    for {
      l <- startupContext match {
        case Startup.Context.Service(_, _, _, _)           => Slf4jLogger.fromName[IO]("grafex-service")
        case Startup.Context.Cli(_, _, verbosity, _, _, _) => Slf4jLogger.fromName[IO](verbosity.asLoggerName)
      }
    } yield new RunContext[IO] {
      override val clock: Clock[IO] = Clock.create[IO]
      override val logger: Logger[IO] = l
    }
  }

  def buildModeContainer(
    startupCtx: Startup.Context,
    runCtx: RunContext[IO],
    config: GrafexConfiguration
  ): Resource[IO, Either[GrafexError, Mode[IO]]] = {
    implicit val rCtx: RunContext[IO] = runCtx

    val metaDataSource = config.graphDataSource match {
      case x: GrafexConfiguration.Foo => new Neo4jMetaDataSource(x)
    }

    buildGraphDataSource(config).evalMap { graphDataSource =>
      IO {
        for {
          dataSourceMode <- Mode.instance(DataSourceMode.definition.toLatest, DataSourceMode(metaDataSource))
          graphMode      <- Mode.instance(GraphMode.definition.toLatest, GraphMode(graphDataSource))
          accountMode    <- Mode.instance(AccountMode.definition.toLatest, AccountMode(graphMode))
          modes <- List(
            dataSourceMode,
            graphMode,
            accountMode
          ).asRight

          describeMode <- Mode.instance(
            DescribeMode.definition.toLatest,
            DescribeMode[IO](
              modes
                .map(_.definition)
                .map(_.asInstanceOf[mode.BasicDefinition[_, _, _]]) // FIXME: unsafe operation
            )
          )

          mainMode <- modes.foldLeft(describeMode)(_ orElse _).asRight
        } yield mainMode
      }
    }
  }

  def buildGraphDataSource(
    config: GrafexConfiguration
  ): Resource[IO, GraphDataSource[IO]] = config.graphDataSource match {
    case GrafexConfiguration.Foo(url) =>
      GraphDatabase
        .driver[IO](url, AuthTokens.none(), Neo4JConfig.builder().withLogging(Neo4JLogging()).build())
        .map(driver => new Neo4JGraphDataSource[IO](driver))
  }

  def launch(
    startupCtx: Startup.Context,
    runCtx: RunContext[IO],
    modeContainer: Mode[IO]
  ): EitherT[IO, GrafexError, ExitCode] = startupCtx match {

    case Startup.Context.Cli(_, _, _, calls, data, options) =>
      for {
        res <- modeContainer(ModeRequest(calls, data, options))
        exitCode <- EitherT.right(res match {
          case ModeResponse.Ok(data, options) =>
            printWithSuccess(if (startupCtx.verbosity >= Verbosity.Verbose) {
              s"""Response options
                 |${if (options.isEmpty) "None"
                 else options.map({ case (k, v) => s"$k=$v" }).reduce((a, b) => s"$a\n$b")}
                 |Response data
                 |$data
                 |""".stripMargin
            } else {
              data
            })
          case ModeResponse.Error(data, code, options) =>
            printWithError(if (startupCtx.verbosity >= Verbosity.Verbose) {
              s"""Response options
                 ${if (options.isEmpty) "None"
                 else options.map({ case (k, v) => s"$k=$v" }).reduce((a, b) => s"$a\n$b")}
                 |Response data
                 |$data
                 |""".stripMargin
            } else {
              data
            })
        })
      } yield exitCode

    case Startup.Context.Service(_, _, _, listeners) =>
      listeners
        .map({
          case Listener.Web()    => WebListener(startupCtx, modeContainer)
          case Listener.Socket() => SocketListener(startupCtx, modeContainer)
        })
        .toList
        .reduce(_ combine _)
  }
}
