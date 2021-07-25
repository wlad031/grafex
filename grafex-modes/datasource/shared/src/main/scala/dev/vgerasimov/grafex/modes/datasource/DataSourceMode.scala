package dev.vgerasimov.grafex
package modes
package datasource

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either._
import dev.vgerasimov.grafex.core.Mode.{ MFunction, ModeInitializationError }
import dev.vgerasimov.grafex.core._
import dev.vgerasimov.grafex.core.conversion.generic.semiauto._
import dev.vgerasimov.grafex.core.conversion.{
  ActionRequestDecoder,
  ActionResponseEncoder,
  ModeRequestDecoder,
  ModeResponseEncoder
}
import dev.vgerasimov.grafex.core.definitions.annotations.{ actionId, modeId }
import dev.vgerasimov.grafex.core.definitions.implicits.all._
import dev.vgerasimov.grafex.core.definitions.syntax.ActionDefinitionOps
import dev.vgerasimov.grafex.core.definitions.{ action, mode }
import dev.vgerasimov.grafex.modes.datasource.DataSourceMode._
import dev.vgerasimov.grafex.modes.datasource.DataSourceMode.actions.GetDataSourceMeta
import io.circe.generic.auto._

class DataSourceMode private (metaDataSource: MetaDataSource[IO])(implicit runContext: RunContext[IO])
    extends MFunction[IO, Request, Response] {

  override def apply(
    request: Request
  ): EitherET[IO, Response] = {
    implicit val mds: MetaDataSource[IO] = metaDataSource
    (request match {
      case req: GetDataSourceMeta.Request => GetDataSourceMeta(req)
    }).map(res => res: Response)
  }
}

@modeId(name = "data-source", version = "1")
object DataSourceMode {
  implicit val definition: mode.BasicDefinition[this.type, Request, Response] = mode.Definition.instance(
    Set(
      GetDataSourceMeta.definition.asDecodable
    )
  )

  def apply(
    metaDataSource: MetaDataSource[IO]
  )(implicit rc: RunContext[IO]): Either[ModeInitializationError, DataSourceMode] = {
    new DataSourceMode(metaDataSource).asRight
  }

  sealed trait Request
  sealed trait Response

  object actions {

    @actionId("get")
    object GetDataSourceMeta {
      implicit val definition: action.Definition[this.type, Request, Response] = null

      def apply(request: Request)(
        implicit metaDataSource: MetaDataSource[IO]
      ): EitherET[IO, Response] = {
        // TODO: reimplement in a safe way
        metaDataSource.getDataSourceById(request.id).value.unsafeRunSync() match {
          case Left(error) => ???
          case Right(value) =>
            EitherT.rightT(
              Response.Ok(
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

      final case class Request(id: String) extends DataSourceMode.Request
      object Request {
        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder[Request].asDefault
      }

      sealed trait Response extends DataSourceMode.Response
      object Response {
        final case class Ok(connection: DSConnection) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }

        final case class Error() extends Response
        object Error {
          implicit val enc: ActionResponseEncoder[Error] = deriveJsonActionResponseEncoder[Error].asDefault
        }
      }

      final case class DSConnection(id: String, `type`: String)
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = deriveModeRequestDecoder
}
