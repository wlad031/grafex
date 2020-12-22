package com.grafex.core

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.{ Clock, IO }
import cats.syntax.either._
import com.grafex.core.Mode._
import com.grafex.core.conversion.{ ModeRequestDecoder, ModeResponseEncoder }
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite

class ModeTest extends AnyFunSuite {
  import ModeTest._

  test("Basic mode should return ByModeNameOrVersion error for wrong mode name") {
    createMode("mode")()()
      .apply(
        Mode.Request(
          NonEmptyList(
            Call.Full(
              Mode.Key(Mode.Name("other-mode"), Mode.Version("1")),
              Mode.Action.Key(Mode.Action.Name("test-action"))
            ),
            Nil
          ),
          """
            |{
            |  "a": 1
            |}""".stripMargin,
          InputType.Json,
          OutputType.Json
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(InvalidRequest.InappropriateCall.ByModeNameOrVersion(_, _)) => // succeed
      case Left(_)                                                          => fail("Got wrong error")
      case Right(_)                                                         => fail("Got successful result")
    }
  }

  test("Basic mode should return ByModeNameOrVersion error for wrong mode version") {
    createMode("mode", "1")()()
      .apply(
        Mode.Request(
          NonEmptyList(
            Call.Full(
              Mode.Key(Mode.Name("other-mode"), Mode.Version("2")),
              Mode.Action.Key(Mode.Action.Name("test-action"))
            ),
            Nil
          ),
          """
            |{
            |  "a": 1
            |}""".stripMargin,
          InputType.Json,
          OutputType.Json
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(InvalidRequest.InappropriateCall.ByModeNameOrVersion(_, _)) => // succeed
      case Left(_)                                                          => fail("Got wrong error")
      case Right(_)                                                         => fail("Got successful result")
    }
  }

  test("Basic mode should return success result for appropriate single call") {
    createMode("test-mode")()()
      .apply(
        Mode.Request(
          NonEmptyList(
            Call.Full(
              Mode.Key(Mode.Name("test-mode"), Mode.Version("1")),
              Mode.Action.Key(Mode.Action.Name("test-action"))
            ),
            Nil
          ),
          """
            |{
            |  "a": 1
            |}""".stripMargin,
          InputType.Json,
          OutputType.Json
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(_)                    => fail("Got error response")
      case Right(Mode.Response(body)) => assert(body.asJson.asObject == """{"a":"test-mode-1"}""".asJson.asObject)
    }
  }

  test("OrElse mode should choose requested mode from 2 basic modes") {
    val left = createMode("left-mode")()()
    val right = createMode("right-mode")()()
    val leftOrRight = left orElse right
    val request = (mode: String) =>
      Mode.Request(
        NonEmptyList(
          Call.Full(Mode.Key(Mode.Name(mode), Mode.Version("1")), Mode.Action.Key(Mode.Action.Name("test-action"))),
          Nil
        ),
        """
        |{
        |  "a": 1
        |}""".stripMargin,
        InputType.Json,
        OutputType.Json
      )

    leftOrRight.apply(request("left-mode")).value.unsafeRunSync() match {
      case Left(_)                    => fail("Got error response")
      case Right(Mode.Response(body)) => assert(body.asJson.asObject == """{"a":"left-mode-1"}""".asJson.asObject)
    }

    leftOrRight.apply(request("right-mode")).value.unsafeRunSync() match {
      case Left(_)                    => fail("Got error response")
      case Right(Mode.Response(body)) => assert(body.asJson.asObject == """{"a":"right-mode-1"}""".asJson.asObject)
    }
  }
}

object ModeTest {
  case class Req(a: Int)
  case class Res(a: String)

  implicit val cReq: ModeRequestDecoder[Req] = ModeRequestDecoder.instance {
    case ar => decode[Req](ar.body).leftMap(e => RequestFormatError(ar, e))
  }

  implicit val cRes: ModeResponseEncoder[Res] = ModeResponseEncoder.instance { (res: Res, _: Mode.Request) =>
    {
      res match {
        case x => Right(Mode.Response(x.asJson.spaces2))
      }
    }
  }

  implicit val testRunContext: RunContext[IO] = new RunContext[IO] {
    override val clock: Clock[IO] = Clock.create[IO]
    override val logger: Logger[IO] = Slf4jLogger.fromName[IO]("test").unsafeRunSync() // FIXME
  }

  def createMode(modeName: String, modeVersion: String = "1")(
    supportedInputTypes: Set[InputType] = Set(InputType.Json),
    supportedOutputTypes: Set[OutputType] = Set(OutputType.Json)
  )(actionName: String = "test-action"): Mode[IO] =
    Mode.instance[IO, Req, Res](
      Mode.Definition.Basic(
        Mode.Key(Mode.Name(modeName), Mode.Version(modeVersion)),
        None,
        supportedInputTypes,
        supportedOutputTypes,
        Set(
          Action.Definition(Action.Key(Action.Name(actionName)), None, Set(Param("a")))
        )
      ),
      (req: Req) => EitherT.rightT[IO, ModeError](Res(s"$modeName-${req.a.toString}"))
    )
}
