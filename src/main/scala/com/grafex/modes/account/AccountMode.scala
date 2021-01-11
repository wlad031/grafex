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
import com.grafex.core.definitions.annotations.{ actionId, description, modeId }
import com.grafex.core.definitions.generic.auto._
import com.grafex.core.definitions.syntax.ActionDefinitionOps
import com.grafex.core.definitions.{ action, mode }
import com.grafex.core.modeFoo.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core.modeFoo.ModeError.ResponseFormatError
import com.grafex.core.modeFoo.{ Mode, ModeError, ModeResponse }
import com.grafex.modes.account.AccountMode.actions.{ CreateAccountAction, GetAccountDetailsAction }
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

@modeId(name = "account", version = "1")
@description("Manages accounts")
object AccountMode {
  implicit val definition: mode.BasicDefinition = mode.Definition.instance[this.type](
    Set(
      action.Definition
        .instance[CreateAccountAction.type, CreateAccountAction.Request, CreateAccountAction.Response]
        .asDecodable,
      action.Definition
        .instance[GetAccountDetailsAction.type, GetAccountDetailsAction.Request, GetAccountDetailsAction.Response]
        .asDecodable
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

    @actionId(name = "create")
    @description("Creates new account")
    object CreateAccountAction {

      final case class Request(
        @description("Account name") name: String
      ) extends AccountMode.Request

      final case class Response(
        @description("Identifier of newly created account") id: String
      ) extends AccountMode.Response

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

      private final case class CreateNodeRequest(label: String, metadata: Map[String, String])
      private final case class CreateNodeResponse(id: String)

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }

    @actionId(name = "get")
    @description("""Returns account details.
                  | Accepts account ID or account name.""".stripMargin)
    object GetAccountDetailsAction {

      sealed trait Request extends AccountMode.Request
      object Request {
        final case class ById(
          @description("Account identifier to search for") id: String
        ) extends GetAccountDetailsAction.Request
        object ById {
          implicit val dec1: ActionRequestDecoder[ById] = deriveOnlyJsonActionRequestDecoder[ById]
        }

        final case class ByName(
          @description("Account name to search for") name: String
        ) extends GetAccountDetailsAction.Request
        object ByName {
          implicit val dec2: ActionRequestDecoder[ByName] = deriveOnlyJsonActionRequestDecoder[ByName]
        }

        implicit val decodeEvent: io.circe.Decoder[Request] =
          List[io.circe.Decoder[Request]](
            io.circe.Decoder[Request.ById].widen,
            io.circe.Decoder[Request.ByName].widen
          ).reduceLeft(_ or _)
      }

      final case class Response(
        @description("Account identifier") id: String,
        @description("Account name") name: String
      ) extends AccountMode.Response

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

      sealed trait Error
      object Error {
        final case class CannotGetAccountName(id: String) extends AccountMode.Error
      }

      private final case class GetNodeRequest(id: String)
      private final case class GetNodeResponse(id: String, metadata: Map[String, String])

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instanceF
}
