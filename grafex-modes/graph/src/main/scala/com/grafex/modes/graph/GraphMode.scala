package com.grafex.modes
package graph

import cats.effect.Sync
import com.grafex.core.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core._
import com.grafex.core.conversion._
import com.grafex.core.conversion.generic.semiauto._
import com.grafex.core.definitions.annotations.{ actionId, modeId }
import com.grafex.core.definitions.generic.auto._
import com.grafex.core.definitions.implicits.all._
import com.grafex.core.definitions.syntax.ActionDefinitionOps
import com.grafex.core.definitions.{ action, mode }
import com.grafex.core.graph.GraphDataSource
import com.grafex.modes.graph.GraphMode._
import com.grafex.modes.graph.GraphMode.actions.{ CreateNodeAction, GetNodeAction }
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
      implicit val definition: action.Definition[this.type, Request, Response] = deriveActionDefinition

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
      implicit val definition: action.Definition[this.type, Request, Response] = deriveActionDefinition

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
