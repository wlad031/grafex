package com.grafex.modes
package graph

import cats.data.EitherT
import cats.effect.Sync
import com.grafex.core._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import com.grafex.core.definitions.annotations.{ actionId, modeId }
import com.grafex.core.definitions.generic.auto._
import com.grafex.core.definitions.syntax.ActionDefinitionOps
import com.grafex.core.definitions.{ action, mode }
import com.grafex.core.graph.GraphDataSource
import com.grafex.core.modeFoo.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core.modeFoo.ModeError
import com.grafex.modes.graph.GraphMode.actions.{ CreateNodeAction, GetNodeAction }
import io.circe.generic.auto._

class GraphMode[F[_] : Sync : RunContext, A] private (
  graphDataSource: GraphDataSource[F]
) extends MFunction[F, GraphMode.Request, GraphMode.Response] {
  override def apply(request: GraphMode.Request): EitherT[F, ModeError, GraphMode.Response] = {
    implicit val mds: GraphDataSource[F] = graphDataSource
    (request match {
      case req: CreateNodeAction.Request => CreateNodeAction(req)
      case req: GetNodeAction.Request    => GetNodeAction(req)
    }).map(x => x: GraphMode.Response)
  }
}

@modeId(name = "graph", version = "1")
object GraphMode {

  implicit val definition: mode.BasicDefinition = mode.Definition.instance[this.type](
    Set(
      action.Definition.instance[GetNodeAction.type, GetNodeAction.Request, GetNodeAction.Response].asDecodable,
      action.Definition.instance[CreateNodeAction.type, CreateNodeAction.Request, CreateNodeAction.Response].asDecodable
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
    object GetNodeAction {
      def apply[F[_] : Sync](
        request: Request
      )(implicit graphDataSource: GraphDataSource[F]): EitherT[F, ModeError, Response] = {
        graphDataSource
          .getNode(request.id)
          .map(node => Response(node.id, node.labels)) //, node.metadata))
          .leftMap(error => DataSourceError(error.toString))
      }

      final case class Request(id: String) extends GraphMode.Request
      final case class Response(id: String, labels: List[String]) //, metadata: Map[String, String])
          extends GraphMode.Response
      final case class DataSourceError(message: String) extends GraphMode.Error

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }

    @actionId("create")
    object CreateNodeAction {
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
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instanceF
}
