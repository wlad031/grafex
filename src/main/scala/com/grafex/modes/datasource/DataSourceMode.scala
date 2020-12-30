package com.grafex.modes.datasource

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
import com.grafex.core.mode.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core.mode.{ Mode, ModeError }
import com.grafex.core.syntax.ActionRequestOps
import com.grafex.modes.datasource.DataSourceMode.actions
import io.circe.generic.auto._

class DataSourceMode private (metaDataSource: MetaDataSource[IO])(implicit runContext: RunContext[IO])
    extends MFunction[IO, DataSourceMode.Request, DataSourceMode.Response] {

  override def apply(
    request: DataSourceMode.Request
  ): EitherT[IO, ModeError, DataSourceMode.Response] = {
    implicit val mds: MetaDataSource[IO] = metaDataSource
    request match {
      case req: actions.CreateDataSourceMeta.Request => ???
      case req: actions.GetDataSourceMeta.Request    => actions.GetDataSourceMeta(req)
    }
  }
}

object DataSourceMode {
  val definition: Mode.Definition.Basic = Mode.Definition.Basic(
    Mode.Key(Mode.Name("ds-connection"), Mode.Version("1")),
    description = None,
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      actions.GetDataSourceMeta.definition
    )
  )

  def apply(
    metaDataSource: MetaDataSource[IO]
  )(implicit rc: RunContext[IO]): Either[ModeInitializationError, DataSourceMode] = {
    Right(new DataSourceMode(metaDataSource))
  }

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  case class DataSourceError() extends Error

  object actions {

    object CreateDataSourceMeta {
      case class Request() extends DataSourceMode.Request
      case class Response() extends DataSourceMode.Response

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

    object GetDataSourceMeta {
      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("get-ds")),
        None,
        Set()
      )

      def apply(request: Request)(
        implicit metaDataSource: MetaDataSource[IO]
      ): EitherT[IO, ModeError, DataSourceMode.Response] = {
        // TODO: reimplement in a safe way
        metaDataSource.getDataSourceById(request.id).value.unsafeRunSync() match {
          case Left(error) => EitherT.leftT(DataSourceError())
          case Right(value) =>
            EitherT.rightT(
              Response(
                DSConnection(
                  id = value.id.s,
                  `type` = value match {
                    case DataSourceMetadata.Virtual(id) => ???
                  }
                )
              )
            )
        }
      }

      case class Request(id: String) extends DataSourceMode.Request
      case class Response(connection: DSConnection) extends DataSourceMode.Response
      case class DSConnection(id: String, `type`: String)

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instance {
    case req if actions.GetDataSourceMeta.definition.suitsFor(req.calls.head.actionKey) =>
      req.asActionRequest[actions.GetDataSourceMeta.Request]
  }
}
