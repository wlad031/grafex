package com.grafex.modes
package datasource

import cats.data.EitherT
import cats.effect.IO
import com.grafex.core.{ ModeError, _ }
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
import com.grafex.core.Mode.{ MFunction, ModeInitializationError }
import com.grafex.modes.datasource.DataSourceMode.actions
import com.grafex.modes.datasource.DataSourceMode.actions.GetDataSourceMeta
import io.circe.generic.auto._

class DataSourceMode private (metaDataSource: MetaDataSource[IO])(implicit runContext: RunContext[IO])
    extends MFunction[IO, DataSourceMode.Request, DataSourceMode.Response] {

  override def apply(
    request: DataSourceMode.Request
  ): EitherT[IO, ModeError, DataSourceMode.Response] = {
    implicit val mds: MetaDataSource[IO] = metaDataSource
    request match {
      case req: actions.GetDataSourceMeta.Request => actions.GetDataSourceMeta(req)
    }
  }
}

@modeId(name = "data-source", version = "1")
object DataSourceMode {
  implicit val definition: mode.BasicDefinition = mode.Definition.instance[this.type](
    Set(
      action.Definition
        .instance[GetDataSourceMeta.type, GetDataSourceMeta.Request, GetDataSourceMeta.Response]
        .asDecodable
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

    @actionId("get")
    object GetDataSourceMeta {

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
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instanceF
}
