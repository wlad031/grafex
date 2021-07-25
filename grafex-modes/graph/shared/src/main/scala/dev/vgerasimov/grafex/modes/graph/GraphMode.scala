package dev.vgerasimov.grafex
package modes
package graph

import cats.effect.Sync
import dev.vgerasimov.grafex.core.Mode.{MFunction, ModeInitializationError}
import dev.vgerasimov.grafex.core._
import dev.vgerasimov.grafex.core.conversion._
import dev.vgerasimov.grafex.core.conversion.generic.semiauto._
import dev.vgerasimov.grafex.core.definitions.annotations.{actionId, description, modeId}
import dev.vgerasimov.grafex.core.definitions.syntax.ActionDefinitionOps
import dev.vgerasimov.grafex.core.definitions.{action, mode}
import dev.vgerasimov.grafex.core.graph.GraphDataSource
import dev.vgerasimov.grafex.modes.graph.GraphMode._
import dev.vgerasimov.grafex.modes.graph.GraphMode.actions.{CreateNodeAction, GetNodeAction}
import io.circe.generic.auto._

class GraphMode[F[_] : Sync : RunContext] private (
  graphDataSource: GraphDataSource[F]
) extends MFunction[F, Request, Response] {
  override def apply(request: Request): EitherET[F, Response] = {
    implicit val mds: GraphDataSource[F] = graphDataSource
    (request match {
      case req: CreateNodeAction.Request => CreateNodeAction(req)
      case req: GetNodeAction.Request    => GetNodeAction(req)
    }).map(res => res: Response)
  }
}

@modeId(name = "graph", version = "1")
@description("asd")
object GraphMode {
  implicit val definition: mode.BasicDefinition[this.type, Request, Response] = mode.Definition.instance(
    Set(
      GetNodeAction.definition.asDecodable,
      CreateNodeAction.definition.asDecodable
    )
  )

  def apply[F[_] : Sync : RunContext](
    graphDataSource: GraphDataSource[F]
  ): Either[ModeInitializationError, GraphMode[F]] = {
    Right(new GraphMode(graphDataSource))
  }

  sealed trait Request
  sealed trait Response

  object actions {

    @actionId("get")
    object GetNodeAction {
      implicit val definition: action.Definition[this.type, Request, Response] = null

      def apply[F[_] : Sync](
        request: Request
      )(implicit graphDataSource: GraphDataSource[F]): EitherET[F, Response] = {
//        graphDataSource
//          .getNode(request.id)
//          .map(node => Response(node.id, node.labels))
//          .flatMap(???)
        null
      }

      final case class Request(id: String) extends GraphMode.Request
      object Request {
        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder[Request].asDefault
      }

      sealed trait Response extends GraphMode.Response
      object Response {

        final case class Ok(id: String, labels: List[String]) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }

        final case class Error() extends Response
        object Error {
          implicit val enc: ActionResponseEncoder[Error] = deriveJsonActionResponseEncoder[Error].asDefault
        }
      }
    }

    @actionId("create")
    object CreateNodeAction {
      implicit val definition: action.Definition[this.type, Request, Response] = null

      def apply[F[_] : Sync](request: Request): EitherET[F, Response] = {
        ???
      }

      final case class Request(dataSourceId: Int) extends GraphMode.Request
      object Request {
        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder[Request].asDefault
      }

      sealed trait Response extends GraphMode.Response
      object Response {

        final case class Ok(nodeId: String) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }

        final case class Error() extends Response
        object Error {
          implicit val enc: ActionResponseEncoder[Error] = deriveJsonActionResponseEncoder[Error].asDefault
        }
      }
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = deriveModeRequestDecoder
}
