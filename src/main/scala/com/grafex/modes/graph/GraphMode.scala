package com.grafex.modes
package graph

import cats.data.EitherT
import cats.effect.Sync
import com.grafex.core._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.conversion.{ActionRequestDecoder, ActionResponseEncoder, ModeRequestDecoder, ModeResponseEncoder}
import com.grafex.core.definition.annotations.actionId
import com.grafex.core.graph.GraphDataSource
import com.grafex.core.mode.Mode.{MFunction, ModeInitializationError}
import com.grafex.core.mode.{Mode, ModeError}
import com.grafex.core.syntax._
import com.grafex.modes.describe.DescribeMode.actions.ListModeKeys.{Request, Response}
import io.circe.generic.auto._
import com.grafex.core.definition.implicits.all._

class GraphMode[F[_] : Sync : RunContext, A] private (
  graphDataSource: GraphDataSource[F]
) extends MFunction[F, GraphMode.Request, GraphMode.Response] {
  import GraphMode.actions

  override def apply(request: GraphMode.Request): EitherT[F, ModeError, GraphMode.Response] = {
    implicit val mds: GraphDataSource[F] = graphDataSource
    (request match {
      case req: actions.CreateNode.Request => actions.CreateNode(req)
      case req: actions.GetNode.Request    => actions.GetNode(req)
    }).map(x => x: GraphMode.Response)
  }
}

object GraphMode {

  type NodeMetadata = Map[String, Any]

  val definition = com.grafex.core.definition.mode.Definition(
    name = "graph",
    version = "1",
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      actions.GetNode.definition,
      actions.CreateNode.definition
    )
  )

  def apply[F[_] : Sync : RunContext, A](
    graphDataSource: GraphDataSource[F]
  ): Either[ModeInitializationError, GraphMode[F, A]] = {
    Right(new GraphMode(graphDataSource))
  }

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  object actions {

    @actionId("get")
    object GetNode {
      val definition = com.grafex.core.definition.action.Definition.derive[this.type, Request, Response]

      def apply[F[_] : Sync](
        request: Request
      )(implicit graphDataSource: GraphDataSource[F]): EitherT[F, ModeError, Response] = {
        graphDataSource
          .getNode(request.id)
          .map(node => Response(node.id, node.labels))//, node.metadata))
          .leftMap(error => DataSourceError(error.toString))
      }

      final case class Request(id: String) extends GraphMode.Request
      final case class Response(id: String, labels: List[String])//, metadata: Map[String, String])
          extends GraphMode.Response
      final case class DataSourceError(message: String) extends GraphMode.Error

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

    @actionId("create")
    object CreateNode {
      val definition = com.grafex.core.definition.action.Definition.derive[this.type, Request, Response]

      def apply[F[_] : Sync](request: Request): EitherT[F, ModeError, Response] = {
        ???
      }

      case class Request(
        dataSourceId: Option[String]
      ) extends GraphMode.Request

      case class Response(
        nodeId: String
      ) extends GraphMode.Response

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instance {
    case req if actions.GetNode.definition.suitsFor(req.calls.head.actionId) =>
      req.asActionRequest[actions.GetNode.Request]
    case req if actions.CreateNode.definition.suitsFor(req.calls.head.actionId) =>
      req.asActionRequest[actions.CreateNode.Request]
  }
}
