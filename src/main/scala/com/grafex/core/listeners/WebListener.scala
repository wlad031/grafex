package com.grafex.core
package listeners

import cats.data.EitherT
import cats.effect.{ ConcurrentEffect, ExitCode, IO, Resource, Timer }
import com.grafex.core.boot.Startup
import com.grafex.core.GrafexError
import com.grafex.core.ModeResponse
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.RequestId
import org.http4s.util.{ CaseInsensitiveString => CIString }

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

object WebListener {

  def apply(
    startupCtx: Startup.Context,
    modeContainer: Mode[IO]
  )(
    implicit
    timer: Timer[IO],
    CE: ConcurrentEffect[IO]
  ): EitherT[IO, GrafexError, ExitCode] = {
    val middlewares: List[HttpRoutes[IO] => HttpRoutes[IO]] = List(
      RequestId.httpRoutes.apply(CIString("X-Request-ID"), IO(UUID.randomUUID())),
      // TODO: maybe reimplement logger middleware?
      org.http4s.server.middleware.Logger.httpRoutes[IO](logHeaders = true, logBody = true)
    )

    val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
      case req @ POST -> Root / mode / modeVersion / action =>
        for {
          body <- req.as[String]
          res <- modeContainer(
            ModeRequest.web(
              Mode.Call
                .Full(definitions.mode.Id(mode, modeVersion), definitions.action.Id(action)),
              body
            )
          ).value.flatMap({
            case Left(err)                     => BadRequest(err.toString)
            case Right(ModeResponse.Json(res)) => Ok(res.spaces2)
            case Right(value)                  => BadRequest("Invalid response type, sorry") // FIXME
          })
        } yield res
    }

    val webService = middlewares.foldLeft(routes)((r, f) => f apply r).orNotFound

    val exitCodeIO = for {
      banner <- readBanner("banner.txt")
      exitCode <- BlazeServerBuilder(global)
        .bindHttp(8080, "localhost")
        .withHttpApp(webService)
        .withBanner(banner)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    } yield exitCode

    EitherT.right(exitCodeIO)
  }

  private def readBanner(filename: String): IO[Seq[String]] =
    Resource
      .fromAutoCloseable(IO(Source.fromResource(filename)))
      .use(bs => IO(bs.getLines().toSeq))
}
