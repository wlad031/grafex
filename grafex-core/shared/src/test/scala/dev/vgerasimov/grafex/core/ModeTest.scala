package dev.vgerasimov.grafex
package core

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.syntax.either._
import dev.vgerasimov.grafex.core.Mode._
import dev.vgerasimov.grafex.core.conversion.{ModeRequestDecoder, ModeResponseEncoder}
import dev.vgerasimov.grafex.core.errors.{GrafexError, InvalidRequest}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest
import org.scalatest.funsuite.AnyFunSuite

class ModeTest extends AnyFunSuite with ModeTestSuite {

  test("Basic mode should return ByModeNameOrVersion error for wrong mode name") {
    createMode("mode")()
      .apply(
        ModeRequest(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("other-mode", "1"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          List("""
            |{
            |  "a": 1
            |}""".stripMargin)
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
    createMode("mode", "1")()
      .apply(
        ModeRequest(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("other-mode", "2"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          List("""
            |{
            |  "a": 1
            |}""".stripMargin)
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
    createMode("test-mode")()
      .apply(
        ModeRequest(
          NonEmptyList(
            Call.Full(
              definitions.mode.Id("test-mode", "1"),
              definitions.action.Id("test-action")
            ),
            Nil
          ),
          List("""
            |{
            |  "a": 1
            |}""".stripMargin)
        )
      )
      .value
      .unsafeRunSync() match {
      case Left(_)                         => fail("Got error response")
      case Right(ModeResponse.Ok(body, _)) => assertStringJson("""{"a":"test-mode-1"}""", body)
      case Right(value)                    => fail(s"Got invalid response: $value")
    }
  }

  test("OrElse mode should choose requested mode from 2 basic modes") {
    val left = createMode("left-mode")()
    val right = createMode("right-mode")()
    val leftOrRight = left orElse right
    val request = (mode: String) =>
      ModeRequest(
        NonEmptyList(
          Call.Full(definitions.mode.Id(mode, "1"), definitions.action.Id("test-action")),
          Nil
        ),
        List("""
        |{
        |  "a": 1
        |}""".stripMargin)
      )

    leftOrRight.apply(request("left-mode")).value.unsafeRunSync() match {
      case Left(_)                         => fail("Got error response")
      case Right(ModeResponse.Ok(body, _)) => assertStringJson("""{"a":"left-mode-1"}""", body)
      case Right(value)                    => fail(s"Got invalid response: $value")
    }

    leftOrRight.apply(request("right-mode")).value.unsafeRunSync() match {
      case Left(_)                         => fail("Got error response")
      case Right(ModeResponse.Ok(body, _)) => assertStringJson("""{"a":"right-mode-1"}""", body)
      case Right(value)                    => fail(s"Got invalid response: $value")
    }
  }

  def assertStringJson(expected: String, actual: String): scalatest.Assertion = {
    assert(unsafe(parse(expected)) === unsafe(parse(actual)))
  }

  case class Req(a: Int)
  case class Res(a: String)

  val reqJsonDecoder = implicitly[io.circe.Decoder[Req]]

  implicit val cReq: ModeRequestDecoder[Req] = ModeRequestDecoder.instance {
    case ar: ModeRequest =>
      reqJsonDecoder.decodeJson(unsafe(parse(ar.data.head))).leftMap(e => InvalidRequest.InvalidTypeOfJson(e))
  }

  implicit val cRes: ModeResponseEncoder[Res] = ModeResponseEncoder.instance { (_: ModeRequest, res: Res) =>
    {
      res match {
        case x => Right(ModeResponse.Ok(x.asJson.spaces2))
      }
    }
  }

  implicit val testRunContext: RunContext[IO] = createTestRunContext[IO].unsafeRunSync()

  def createMode(modeName: String, modeVersion: String = "1")(actionName: String = "test-action"): Mode[IO] =
    Mode.instance[IO, Any, Req, Res](
      definitions.mode.Definition[Any, Req, Res](
        modeName,
        modeVersion,
        Set()
      ),
      (req: Req) => EitherT.rightT[IO, GrafexError](Res(s"$modeName-${req.a.toString}"))
    )
}
