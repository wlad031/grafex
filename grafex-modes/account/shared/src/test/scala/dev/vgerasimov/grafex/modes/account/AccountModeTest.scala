package dev.vgerasimov.grafex
package modes
package account

import cats.data.EitherT
import cats.effect.IO
import cats.instances.either._
import cats.syntax.bifunctor._
import dev.vgerasimov.grafex.core.conversion.{ModeRequestDecoder, ModeResponseEncoder}
import dev.vgerasimov.grafex.core.errors.InvalidRequest
import dev.vgerasimov.grafex.core.{EitherET, Mode, ModeRequest, ModeResponse, ModeTestSuite, RunContext, unsafe}
import dev.vgerasimov.grafex.modes.account.AccountMode.actions.CreateAccountAction
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite

class AccountModeTest extends AnyFunSuite with ModeTestSuite {

  test("Account mode should create an account") {
    val accountMode: AccountMode[IO] = AccountMode(graphMode).getOrElse(sys.error(""))

    val res = accountMode.apply(CreateAccountAction.Request("account-name")).value.unsafeRunSync()

    res match {
      case Left(error)                                => fail(s"Unexpected error: $error")
      case Right(CreateAccountAction.Response.Ok(id)) => assert(id === "id=account-name")
      case Right(x)                                   => fail(s"Unexpected result: $x")
    }
  }

  sealed trait TestGraphRequest
  sealed trait TestGraphResponse

  case class TestCreateNodeRequest(label: String, metadata: Map[String, String]) extends TestGraphRequest
  case class TestCreateNodeResponse(id: String) extends TestGraphResponse

  implicit val testRunContext: RunContext[IO] = createTestRunContext[IO].unsafeRunSync()

  implicit val enc: ModeResponseEncoder[TestGraphResponse] = ModeResponseEncoder.instance {
    (req: ModeRequest, res: TestGraphResponse) =>
      res match {
        case r: TestCreateNodeResponse => Right(ModeResponse.Ok(r.asJson.spaces2))
      }
  }

  val resJsonDecoder = implicitly[io.circe.Decoder[TestCreateNodeRequest]]

  implicit val dec: ModeRequestDecoder[TestGraphRequest] = ModeRequestDecoder.instance {
    case req if req.calls.head.actionId.name == "node/create" =>
      resJsonDecoder
        .decodeJson(unsafe(parse(req.asInstanceOf[ModeRequest].body.head)))
        .leftMap(x => InvalidRequest.UnsupportedAction(x.toString))
  }

  val graphMode = Mode.instance(
    dev.vgerasimov.grafex.core.definitions.mode.Definition[Any, TestGraphRequest, TestGraphResponse](
      "graph",
      "1",
      Set()
    ),
    new Mode.MFunction[IO, TestGraphRequest, TestGraphResponse] {
      override def apply(request: TestGraphRequest): EitherET[IO, TestGraphResponse] = {
        request match {
          case TestCreateNodeRequest(label, metadata) =>
            EitherT.rightT(TestCreateNodeResponse(s"""id=${metadata("name")}"""))
        }
      }
    }
  )

}
