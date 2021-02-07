package com.grafex.modes
package account

import cats.data.EitherT
import cats.effect.Sync
import cats.instances.either._
import cats.syntax.bifunctor._
import cats.syntax.functor._
import com.grafex.core.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core._
import com.grafex.core.conversion.generic.semiauto._
import com.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import com.grafex.core.definitions.annotations.{ actionId, description, modeId }
import com.grafex.core.definitions.generic.auto._
import com.grafex.core.definitions.implicits.all._
import com.grafex.core.definitions.syntax.ActionDefinitionOps
import com.grafex.core.definitions.{ action, mode }
import com.grafex.modes.account.AccountMode._
import com.grafex.modes.account.AccountMode.actions.{ CreateAccountAction, GetAccountDetailsAction }
import io.circe.generic.auto._
import io.circe.parser.parse

class AccountMode[F[_] : Sync : RunContext] private (graphMode: Mode[F]) extends MFunction[F, Request, Response] {

  override def apply(request: AccountMode.Request): EitherET[F, Response] = {
    implicit val gm: Mode[F] = graphMode
    (request match {
      case req: CreateAccountAction.Request     => CreateAccountAction(req)
      case req: GetAccountDetailsAction.Request => GetAccountDetailsAction(req)
    }).map(x => x: AccountMode.Response)
  }
}

@modeId(name = "account", version = "1")
@description("Manages accounts")
object AccountMode {
  implicit val definition: mode.BasicDefinition[this.type, Request, Response] = mode.Definition.instance(
    Set(
      CreateAccountAction.definition.asDecodable,
      GetAccountDetailsAction.definition.asDecodable
    )
  )

  def apply[F[_] : Sync : RunContext](graphMode: Mode[F]): Either[ModeInitializationError, AccountMode[F]] = {
    if (List(
          CreateAccountAction.createNodeGraphModeCall,
          GetAccountDetailsAction.getNodeGraphModeCall
        ).exists(call => !graphMode.definition.suitsFor(call))) {
      Left(ModeInitializationError.NeededCallUnsupported())
    } else {
      Right(new AccountMode(graphMode))
    }
  }

  sealed trait Request
  sealed trait Response

  object actions {

    @actionId(name = "create")
    @description("Creates new account")
    object CreateAccountAction {
      implicit val definition: action.Definition[this.type, Request, Response] = deriveActionDefinition

      final case class Request(
        @description("Account name") name: String
      ) extends AccountMode.Request
      object Request {
        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder[Request].asDefault
      }

      sealed trait Response extends AccountMode.Response
      object Response {

        final case class Ok(
          @description("Identifier of newly created account") id: String
        ) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }
      }

      val createNodeGraphModeCall: Mode.Call = unsafeParseSingleModeCall("graph.1/node/create")

      def apply[F[_] : Sync : RunContext](
        request: Request
      )(implicit graphMode: Mode[F]): EitherET[F, Response] = {
        val graphRequest = {
          ModeClient.jsonRequest(
            createNodeGraphModeCall,
            CreateNodeRequest(Node.labels.Account, Map("name" -> request.name))
          )
        }
        val jsonDecoder = implicitly[io.circe.Decoder[CreateNodeResponse]]
        for {
          graphRes <- graphMode.apply(graphRequest)
          jsonRes <- EitherT.fromEither[F](graphRes match {
            case ModeResponse.Ok(body, _)       => parse(body).leftMap(e => ResponseFormatError(graphRes, e))
            case ModeResponse.Error(data, _, _) => ???
          })
          r <- EitherT.fromEither[F](
            jsonDecoder
              .decodeJson(jsonRes)
              .leftMap(e => ResponseFormatError(graphRes, e): GrafexError)
          )
        } yield {
          Response.Ok(r.id)
        }
      }

      private final case class CreateNodeRequest(label: String, metadata: Map[String, String])
      private final case class CreateNodeResponse(id: String)
    }

    @actionId(name = "get")
    @description("""Returns account details.
                   |Accepts account ID or account name.""".stripMargin)
    object GetAccountDetailsAction {
      implicit val definition: action.Definition[this.type, Request, Response] = deriveActionDefinition

      sealed trait Request extends AccountMode.Request
      object Request {
        final case class ById(
          @description("Account identifier to search for") id: String
        ) extends Request
        object ById {
          implicit val dec: ActionRequestDecoder[ById] =
            deriveJsonActionRequestDecoder[ById](definition.id).asDefault
        }

        final case class ByName(
          @description("Account name to search for") name: String
        ) extends Request
        object ByName {
          implicit val dec: ActionRequestDecoder[ByName] =
            deriveJsonActionRequestDecoder[ByName](definition.id).asDefault
        }

        implicit val decodeEvent: io.circe.Decoder[Request] =
          List[io.circe.Decoder[Request]](
            io.circe.Decoder[Request.ById].widen,
            io.circe.Decoder[Request.ByName].widen
          ).reduceLeft(_ or _)

        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder
      }

      sealed trait Response extends AccountMode.Response
      object Response {

        final case class Ok(
          @description("Account identifier") id: String,
          @description("Account name") name: String
        ) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }
      }

      val getNodeGraphModeCall: Mode.Call = unsafeParseSingleModeCall("graph.1/node/get")

      def apply[F[_] : Sync : RunContext](
        request: Request
      )(implicit graphMode: Mode[F]): EitherET[F, Response] = {
//        val graphRequest = request match {
//          case Request.ById(id) =>
//            ModeClient.jsonRequest(
//              getNodeGraphModeCall,
//              GetNodeRequest(id)
//            )
//          case Request.ByName(name) => ???
//        }
//        val jsonDecoder = implicitly[io.circe.Decoder[GetNodeResponse]]
//        for {
//          graphRes <- graphMode.apply(graphRequest)
//          jsonRes <- graphRes match {
//            case ModeResponse1.Json(body) => body.asRight[ModeError1].toEitherT[F]
//          }
//          r <- jsonDecoder
//            .decodeJson(jsonRes)
//            .leftMap(e => ResponseFormatError(graphRes, e): ModeError1)
//            .toEitherT[F]
//          name <- EitherT.fromOption[F](r.metadata.get("name"), ifNone = Error.CannotGetAccountName(r.id): ModeError1)
//        } yield {
//          Response(r.id, name)
//        }
        ???
      }

      private final case class GetNodeRequest(id: String)
      private final case class GetNodeResponse(id: String, metadata: Map[String, String])
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = deriveModeRequestDecoder
}
