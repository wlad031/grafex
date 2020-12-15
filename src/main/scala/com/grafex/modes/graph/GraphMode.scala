package com.grafex.modes.graph

import cats.data.EitherT
import cats.effect.IO
import com.grafex.core._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import com.grafex.core.syntax._
import io.circe.generic.auto._

class GraphMode(metaDataSource: MetaDataSource[IO]) extends Mode.MFunction[IO, GraphMode.Request, GraphMode.Response] {
  import GraphMode.actions

  override def apply(request: GraphMode.Request): EitherT[IO, ModeError, GraphMode.Response] = {
    implicit val mds: MetaDataSource[IO] = metaDataSource
    (request match {
      case req: actions.CreateNode.Request => actions.CreateNode(req)
      case req: actions.GetNode.Request    => actions.GetNode(req)
    }).map(x => x: GraphMode.Response)
  }
}

object GraphMode {

  val definition: Mode.Definition.Basic = Mode.Definition.Basic(
    Mode.Key(Mode.Name("graph"), Mode.Version("1")),
    None,
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(actions.GetNode.definition)
  )

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  object actions {

    object GetNode {
      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("get-node")),
        None,
        Set()
      )

      def apply(request: Request)(implicit metaDataSource: MetaDataSource[IO]): EitherT[IO, ModeError, Response] = {
        ???
      }

      case class Request() extends GraphMode.Request
      case class Response() extends GraphMode.Response

      sealed trait DataSourceConnection {
        def id: DataSourceMetadata.Id
      }

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

    object CreateNode {
      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("create-node")),
        None,
        Set()
      )

      def apply(request: Request): EitherT[IO, ModeError, Response] = {
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
    case req if actions.GetNode.definition.suitsFor(req.call.actionKey) =>
      req.asActionRequest[actions.GetNode.Request](req.inputType)
    case req if actions.CreateNode.definition.suitsFor(req.call.actionKey) =>
      req.asActionRequest[actions.CreateNode.Request](req.inputType)
  }
}
