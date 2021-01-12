package com.grafex.core

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.IO
import cats.syntax.either._
import com.grafex.core.conversion.{ ModeRequestDecoder, ModeResponseEncoder }
import com.grafex.core.Mode._
import ModeError.{ InvalidRequest, RequestFormatError }
import com.grafex.core.ModeError
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite

class ModeTest extends AnyFunSuite with ModeTestSuite {

  test("Basic mode should return ByModeNameOrVersion error for wrong mode name") {
    createMode("mode")()()
      .apply(
        ModeRequest.Json(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("other-mode", "1"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          OutputType.Json,
          parse("""
            |{
            |  "a": 1
            |}""".stripMargin).getOrElse(???) // FIXME
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(InvalidRequest.WrongMode(_, _)) => // succeed
      case Left(_)                              => fail("Got wrong error")
      case Right(_)                             => fail("Got successful result")
    }
  }

  test("Basic mode should return ByModeNameOrVersion error for wrong mode version") {
    createMode("mode", "1")()()
      .apply(
        ModeRequest.Json(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("other-mode", "2"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          OutputType.Json,
          parse("""
            |{
            |  "a": 1
            |}""".stripMargin).getOrElse(???) // FIXME
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(InvalidRequest.WrongMode(_, _)) => // succeed
      case Left(_)                              => fail("Got wrong error")
      case Right(_)                             => fail("Got successful result")
    }
  }

  test("Basic mode should return success result for appropriate single call") {
    createMode("test-mode")()()
      .apply(
        ModeRequest.Json(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("test-mode", "1"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          OutputType.Json,
          parse("""
            |{
            |  "a": 1
            |}""".stripMargin).getOrElse(???) // FIXME
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(_)                        => fail("Got error response")
      case Right(ModeResponse.Json(body)) => assert(parse("""{"a":"test-mode-1"}""").getOrElse(???) === body.asJson)
      case Right(value)                   => fail(s"Got invalid response: $value")
    }
  }

  test("OrElse mode should choose requested mode from 2 basic modes") {
    val left = createMode("left-mode")()()
    val right = createMode("right-mode")()()
    val leftOrRight = left orElse right
    val request = (mode: String) =>
      ModeRequest.Json(
        NonEmptyList(
          Call.Full(definitions.mode.Id(mode, "1"), definitions.action.Id("test-action")),
          Nil
        ),
        OutputType.Json,
        parse("""
        |{
        |  "a": 1
        |}""".stripMargin).getOrElse(???) // FIXME
      )

    leftOrRight.apply(request("left-mode")).value.unsafeRunSync() match {
      case Left(_)                        => fail("Got error response")
      case Right(ModeResponse.Json(body)) => assert(parse("""{"a":"left-mode-1"}""").getOrElse(???) === body.asJson)
      case Right(value)                   => fail(s"Got invalid response: $value")
    }

    leftOrRight.apply(request("right-mode")).value.unsafeRunSync() match {
      case Left(_)                        => fail("Got error response")
      case Right(ModeResponse.Json(body)) => assert(parse("""{"a":"right-mode-1"}""").getOrElse(???) === body.asJson)
      case Right(value)                   => fail(s"Got invalid response: $value")
    }
  }

  case class Req(a: Int)
  case class Res(a: String)

  val reqJsonDecoder = implicitly[io.circe.Decoder[Req]]

  implicit val cReq: ModeRequestDecoder[Req] = ModeRequestDecoder.instance {
    case ar: ModeRequest.Json => reqJsonDecoder.decodeJson(ar.body).leftMap(e => RequestFormatError(ar, e))
  }

  implicit val cRes: ModeResponseEncoder[Res] = ModeResponseEncoder.instance { (res: Res, _: ModeRequest) =>
    {
      res match {
        case x => Right(ModeResponse.Json(x.asJson))
      }
    }
  }

  implicit val testRunContext: RunContext[IO] = createTestRunContext[IO].unsafeRunSync()

  def createMode(modeName: String, modeVersion: String = "1")(
    supportedInputTypes: Set[InputType] = Set(InputType.Json),
    supportedOutputTypes: Set[OutputType] = Set(OutputType.Json)
  )(actionName: String = "test-action"): Mode[IO] =
    Mode.instance[IO, Req, Res](
      definitions.mode.Definition(
        modeName,
        modeVersion,
        supportedInputTypes,
        supportedOutputTypes,
        Set()
      ),
      (req: Req) => EitherT.rightT[IO, ModeError](Res(s"$modeName-${req.a.toString}"))
    )
}
