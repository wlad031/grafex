package com.grafex
package modes
package account

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.IO
import com.grafex.core.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import com.grafex.core.syntax.ActionRequestOps
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

class AccountMode private (graphMode: Mode[IO])(implicit runContext: RunContext[IO])
    extends MFunction[IO, AccountMode.Request, AccountMode.Response] {
  import AccountMode.actions

  override def apply(request: AccountMode.Request): EitherT[IO, ModeError, AccountMode.Response] = {
    implicit val gm: Mode[IO] = graphMode
    (request match {
      case req: actions.CreateAccountAction.Request => actions.CreateAccountAction(req)
    }).map(x => x: AccountMode.Response)
  }
}

object AccountMode {

  val definition: Mode.Definition.Basic = Mode.Definition.Basic(
    Mode.Key(Mode.Name("account"), Mode.Version("1")),
    Some("""
        |Manages accounts.
        |""".stripMargin),
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      actions.CreateAccountAction.definition
    )
  )

  def apply(graphMode: Mode[IO])(implicit rc: RunContext[IO]): Either[ModeInitializationError, AccountMode] = {
    if (List(
          actions.CreateAccountAction.createNodeGraphModeCall
        ).exists(call => !graphMode.definition.suitsFor(call, InputType.Json, OutputType.Json))) {
      Left(ModeInitializationError.NeededCallUnsupported())
    } else {
      Right(new AccountMode(graphMode))
    }
  }

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  object actions {

    object CreateAccountAction {

      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("create-account")),
        Some("""
            |Creates new account.
            |""".stripMargin),
        Set()
      )

      val createNodeGraphModeCall = unsafeParseSingleModeCall("graph.1/create-node")

      def apply(request: Request)(implicit graphMode: Mode[IO]): EitherT[IO, ModeError, Response] = {
        val call = Mode.Call.Full(
          Mode.Key(Mode.Name("graph"), Mode.Version("1")),
          Mode.Action.Key(Mode.Action.Name("create-node"))
        )
        val createNodeRequest = Mode.Request(
          NonEmptyList(call, Nil),
          CreateNodeRequest(Node.labels.AccountLabel).asJson.noSpaces,
          InputType.Json,
          OutputType.Json
        )

        ???
      }

      case class Request(name: String) extends AccountMode.Request
      case class Response(id: String, name: String) extends AccountMode.Response

      private case class CreateNodeRequest(label: String)

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instance {
    case req if actions.CreateAccountAction.definition.suitsFor(req.call.actionKey) =>
      req.asActionRequest[actions.CreateAccountAction.Request](req.inputType)
  }
}
