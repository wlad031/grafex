package com.grafex.modes
package account

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import com.grafex.core._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import com.grafex.core.mode.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core.mode.ModeError.ResponseFormatError
import com.grafex.core.mode.{ Mode, ModeError, ModeResponse }
import com.grafex.core.syntax.ActionRequestOps
import io.circe.generic.auto._

class AccountMode[F[_] : Sync : RunContext] private (graphMode: Mode[F])
    extends MFunction[F, AccountMode.Request, AccountMode.Response] {
  import AccountMode.actions

  override def apply(request: AccountMode.Request): EitherT[F, ModeError, AccountMode.Response] = {
    implicit val gm: Mode[F] = graphMode
    (request match {
      case req: actions.CreateAccountAction.Request     => actions.CreateAccountAction(req)
      case req: actions.GetAccountDetailsAction.Request => actions.GetAccountDetailsAction(req)
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
      actions.CreateAccountAction.definition,
      actions.GetAccountDetailsAction.definition
    )
  )

  def apply[F[_] : Sync : RunContext](graphMode: Mode[F]): Either[ModeInitializationError, AccountMode[F]] = {
    if (List(
          actions.CreateAccountAction.createNodeGraphModeCall,
          actions.GetAccountDetailsAction.getNodeGraphModeCall
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
        Mode.Action.Key(Mode.Action.Name("create")),
        Some("""
            |Creates new account.
            |""".stripMargin),
        Set()
      )

      val createNodeGraphModeCall: Mode.Call = unsafeParseSingleModeCall("graph.1/node/create")

      def apply[F[_] : Sync : RunContext](
        request: Request
      )(implicit graphMode: Mode[F]): EitherT[F, ModeError, Response] = {
        val graphRequest = {
          ModeClient.jsonRequest(
            createNodeGraphModeCall,
            CreateNodeRequest(Node.labels.Account, Map("name" -> request.name))
          )
        }
        val jsonDecoder = implicitly[io.circe.Decoder[CreateNodeResponse]]
        for {
          graphRes <- graphMode.apply(graphRequest)
          jsonRes <- graphRes match {
            case ModeResponse.Json(body) => body.asRight[ModeError].toEitherT[F]
          }
          r <- jsonDecoder
            .decodeJson(jsonRes)
            .leftMap(e => ResponseFormatError(graphRes, e): ModeError)
            .toEitherT[F]
        } yield {
          Response(r.id)
        }
      }

      final case class Request(name: String) extends AccountMode.Request
      final case class Response(id: String) extends AccountMode.Response

      private final case class CreateNodeRequest(label: String, metadata: Map[String, String])
      private final case class CreateNodeResponse(id: String)

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

    object GetAccountDetailsAction {

      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("get")),
        Some("""
               |Returns account details.
               |""".stripMargin),
        Set()
      )

      val getNodeGraphModeCall: Mode.Call = unsafeParseSingleModeCall("graph.1/node/get")

      def apply[F[_] : Sync : RunContext](
        request: Request
      )(implicit graphMode: Mode[F]): EitherT[F, ModeError, Response] = {
        val graphRequest = request match {
          case Request.ById(id) =>
            ModeClient.jsonRequest(
              getNodeGraphModeCall,
              GetNodeRequest(id)
            )
          case Request.ByName(name) => ???
        }
        val jsonDecoder = implicitly[io.circe.Decoder[GetNodeResponse]]
        for {
          graphRes <- graphMode.apply(graphRequest)
          jsonRes <- graphRes match {
            case ModeResponse.Json(body) => body.asRight[ModeError].toEitherT[F]
          }
          r <- jsonDecoder
            .decodeJson(jsonRes)
            .leftMap(e => ResponseFormatError(graphRes, e): ModeError)
            .toEitherT[F]
          name <- EitherT.fromOption[F](r.metadata.get("name"), ifNone = Error.CannotGetAccountName(r.id): ModeError)
        } yield {
          Response(r.id, name)
        }
      }

      sealed trait Request extends AccountMode.Request
      object Request {
        final case class ById(id: String) extends GetAccountDetailsAction.Request
        object ById {
          implicit val dec: ActionRequestDecoder[ById] = deriveOnlyJsonActionRequestDecoder
        }

        final case class ByName(name: String) extends GetAccountDetailsAction.Request
        object ByName {
          implicit val dec: ActionRequestDecoder[ByName] = deriveOnlyJsonActionRequestDecoder
        }

        implicit val decodeEvent: io.circe.Decoder[Request] =
          List[io.circe.Decoder[Request]](
            io.circe.Decoder[Request.ById].widen,
            io.circe.Decoder[Request.ByName].widen
          ).reduceLeft(_ or _)
      }

      final case class Response(id: String, name: String) extends AccountMode.Response

      sealed trait Error
      object Error {
        final case class CannotGetAccountName(id: String) extends AccountMode.Error
      }

      private final case class GetNodeRequest(id: String)
      private final case class GetNodeResponse(id: String, metadata: Map[String, String])

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instance {
    case req if actions.CreateAccountAction.definition.suitsFor(req.calls.head.actionKey) =>
      req.asActionRequest[actions.CreateAccountAction.Request]
    case req if actions.GetAccountDetailsAction.definition.suitsFor(req.calls.head.actionKey) =>
      req.asActionRequest[actions.GetAccountDetailsAction.Request]
  }
}
