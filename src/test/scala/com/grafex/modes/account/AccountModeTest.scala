package com.grafex.modes
package account

import cats.data.EitherT
import cats.effect.IO
import cats.instances.either._
import cats.syntax.bifunctor._
import com.grafex.core._
import com.grafex.core.conversion.{ ModeRequestDecoder, ModeResponseEncoder }
import com.grafex.modes.describe.DescribeMode.UnknownModeError
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite

class AccountModeTest extends AnyFunSuite with ModeTestSuite {

  test("Account mode should create an account") {
    val accountMode: AccountMode[IO] = AccountMode(graphMode).getOrElse(sys.error(""))

    val res = accountMode.apply(AccountMode.actions.CreateAccountAction.Request("account-name")).value.unsafeRunSync()

    res match {
      case Left(error)                                                 => fail(s"Unexpected error: $error")
      case Right(AccountMode.actions.CreateAccountAction.Response(id)) => assert(id === "id=account-name")
      case Right(x)                                                    => fail(s"Unexpected result: $x")
    }
  }

  sealed trait TestGraphRequest
  sealed trait TestGraphResponse

  case class TestCreateNodeRequest(label: String, metadata: Map[String, String]) extends TestGraphRequest
  case class TestCreateNodeResponse(id: String) extends TestGraphResponse

  implicit val testRunContext: RunContext[IO] = createTestRunContext[IO].unsafeRunSync()

  implicit val enc: ModeResponseEncoder[TestGraphResponse] = ModeResponseEncoder.instance {
    (res: TestGraphResponse, req: Mode.Request) =>
      res match {
        case r: TestCreateNodeResponse => Right(Mode.Response(r.asJson.noSpaces))
      }
  }

  implicit val dec: ModeRequestDecoder[TestGraphRequest] = ModeRequestDecoder.instance {
    case req if req.call.actionKey.name.toString == "node/create" =>
      decode[TestCreateNodeRequest](req.body).leftMap(x => UnknownModeError(x.toString): ModeError)
  }

  val graphMode = Mode.instance(
    Mode.Definition.Basic(
      Mode.Key(Mode.Name("graph"), Mode.Version("1")),
      None,
      Set(InputType.Json),
      Set(OutputType.Json),
      Set(
        Mode.Action.Definition(
          Mode.Action.Key(Mode.Action.Name("create")),
          None,
          Set()
        )
      )
    ),
    new Mode.MFunction[IO, TestGraphRequest, TestGraphResponse] {
      override def apply(request: TestGraphRequest): EitherT[IO, ModeError, TestGraphResponse] = {
        request match {
          case TestCreateNodeRequest(label, metadata) =>
            EitherT.rightT(TestCreateNodeResponse(s"""id=${metadata("name")}"""))
        }
      }
    }
  )

}
