package dev.vgerasimov.grafex
package modes
package describe

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.either._
import dev.vgerasimov.grafex.core.Mode.{MFunction, ModeInitializationError}
import dev.vgerasimov.grafex.core._
import dev.vgerasimov.grafex.core.conversion._
import dev.vgerasimov.grafex.core.conversion.generic.semiauto._
import dev.vgerasimov.grafex.core.definitions.annotations.{actionId, description, modeId}
import dev.vgerasimov.grafex.core.definitions.implicits.deriveActionDefinition
import dev.vgerasimov.grafex.core.definitions.syntax.ActionDefinitionOps
import dev.vgerasimov.grafex.core.definitions.{action, mode}
import dev.vgerasimov.grafex.core.errors.InvalidRequest
import dev.vgerasimov.grafex.modes.describe.DescribeMode._
import dev.vgerasimov.grafex.modes.describe.DescribeMode.actions.{GetModeDefinitionAction, ListModeKeysAction}
import dev.vgerasimov.shapelse.names.NameShapeEncoder
import dev.vgerasimov.shapelse.{names, typenames, values}
import io.circe.generic.auto._

class DescribeMode[F[_] : Sync : RunContext] private (
  otherDefinitions: => Seq[definitions.mode.Callable],
  amILatest: Boolean = true
) extends MFunction[F, Request, Response] {

  private[this] lazy val myDefinition = if (amILatest) DescribeMode.definition.toLatest else DescribeMode.definition
  private[this] lazy val fullMetadata: DescribeMode.Metadata = otherDefinitions.map(DescribeMode.Metadata(_))
     .foldLeft(DescribeMode.Metadata(myDefinition))(_ ++ _)

  override def apply(request: Request): EitherET[F, Response] = {
    implicit val fm: DescribeMode.Metadata = fullMetadata
    request match {
      case req: ListModeKeysAction.Request      => EitherT.fromEither(ListModeKeysAction(req))
      case req: GetModeDefinitionAction.Request => EitherT.fromEither(GetModeDefinitionAction(req))
    }
  }
}

import dev.vgerasimov.shapelse.empty.implicits.all._
import dev.vgerasimov.shapelse.empty.instances._
import dev.vgerasimov.shapelse.annotations.implicits.all._
import names.implicits.all._
import typenames.implicits.all._
import values.implicits.all._
import dev.vgerasimov.grafex.core.definitions.implicits._

@modeId(name = "describe", version = "1")
object DescribeMode {
  implicit val definition: mode.BasicDefinition[this.type, Request, Response] = mode.Definition.instance(
    Set(
      ListModeKeysAction.definition.asDecodable,
      GetModeDefinitionAction.definition.asDecodable
    )
  )

  def apply[F[_] : Sync : RunContext](
    otherDefinitions: => Seq[mode.Callable],
    amILatest: Boolean = true
  ): Either[ModeInitializationError, DescribeMode[F]] = {
    new DescribeMode(otherDefinitions, amILatest).asRight
  }

  sealed trait Request
  sealed trait Response

  object actions {

    @actionId(name = "list-mode-keys")
    @description(
      """Returns list of available modes with their names and versions.
        |Supports `format` option:
        |  - `short` (default): returns JSON array, for each mode string "<name>.<version>";
        |  - `full`: returns JSON array, for each mode JSON object with
        |            `name`, `version` and `isLatest` fields.""".stripMargin
    )
    object ListModeKeysAction {
      implicit val definition: action.Definition[this.type, Request, Response] = deriveActionDefinition[this.type, Request, Response]

      def apply(request: Request)(implicit fullMetadata: Metadata): EitherE[Response] =
        request match {
          case Request.FullFormat =>
            Response
              .Full(
                fullMetadata.definitionsMap
                  .map({ case (k, d) => ModeKey(k.name, k.version, d.isLatest) })
                  .toList
              )
              .asRight
          case Request.ShortFormat =>
            Response
              .Short(
                fullMetadata.definitionsMap
                  .map({ case (k, _) => s"${k.name}.${k.version}" })
                  .toList
              )
              .asRight
        }

      sealed trait Request extends DescribeMode.Request
      object Request {
        final case object ShortFormat extends Request
        final case object FullFormat extends Request

        implicit val dec: ActionRequestDecoder[Request] =
          ActionRequestDecoder.instance { (req: ModeRequest) =>
            {
              req.options
                .get("format")
                .map({
                  case "short" => Request.ShortFormat.asRight
                  case "full"  => Request.FullFormat.asRight
                  case s       => InvalidRequest.UnsupportedOption("format", s).asLeft
                })
                .getOrElse(Request.ShortFormat.asRight)
            }
          }(definition.id)
      }

      sealed trait Response extends DescribeMode.Response
      object Response {

        final case class Full(modeKeys: List[ModeKey]) extends Response
        object Full {
          implicit val enc: ActionResponseEncoder[Full] = deriveJsonActionResponseEncoder[Full].asDefault
        }

        final case class Short(modeKeys: List[String]) extends Response
        object Short {
          implicit val enc: ActionResponseEncoder[Short] = deriveJsonActionResponseEncoder[Short].asDefault
        }
      }

      final case class ModeKey(name: String, version: String, isVersionLatest: Boolean)
    }

    @actionId("get-def")
    object GetModeDefinitionAction {
      implicit val definition: action.Definition[this.type, Request, Response] =
        deriveActionDefinition[this.type, Request, Response]

      def apply(request: Request)(implicit fullMetadata: Metadata): EitherE[Response] = {
        val name = request.modeName
        val version = request.modeVersion
        fullMetadata
          .get(name, version)
          .map(_.asInstanceOf[mode.BasicDefinition[_, _, _]]) // FIXME: unsafe operation
          .map(ModeDefinition.apply)
          .map(Response.Ok.apply)
          .getOrElse(Response.UnknownMode(name, version))
          .asRight
      }

      final case class Request(
                                @description("Name of the mode") modeName: String,
                                @description("Version of the mode") modeVersion: Option[String]
                              ) extends DescribeMode.Request
      object Request {
        implicit val dec: ActionRequestDecoder[Request] = deriveJsonActionRequestDecoder[Request].asDefault
      }

      sealed trait Response extends DescribeMode.Response
      object Response {

        final case class Ok(definition: ModeDefinition) extends Response
        object Ok {
          implicit val enc: ActionResponseEncoder[Ok] = deriveJsonActionResponseEncoder[Ok].asDefault
        }

        final case class UnknownMode(modeName: String, version: Option[String])
            extends ErrorMeta(ErrorCode.ClientError)
            with Response
        object UnknownMode {
          implicit val enc: ActionResponseEncoder[UnknownMode] =
            deriveJsonActionErrorEncoder[UnknownMode].asDefault
        }
      }

      final case class ModeDefinition(
        name: String,
        description: Option[String],
        version: String,
        actions: List[ActionDefinition]
      )

      object ModeDefinition {
        def apply(md: mode.BasicDefinition[_, _, _]): ModeDefinition = {
          ModeDefinition(
            md.id.name,
            md.description,
            md.id.version,
            md.actions
              .map(ad =>
                ActionDefinition(
                  ad.actionDefinition.id.name,
                  ad.actionDefinition.description,
                  List() // FIXME
//                  ad.actionDefinition.input.params.map(pd => ParamDefinition(pd.name.toString)).toList
                )
              )
              .toList
          )
        }
      }

      final case class ActionDefinition(name: String, description: Option[String], params: List[ParamDefinition])
      final case class ParamDefinition(name: String)
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = deriveModeRequestDecoder

  /** Encapsulates the map containing mode definitions in convenient for searching way. */
  private class Metadata(val definitionsMap: Map[Metadata.Key, dev.vgerasimov.grafex.core.definitions.mode.Callable]) {

    /** Returns new metadata which contains definitions from this and provided metadata. */
    def ++ (that: Metadata): Metadata = new Metadata(this.definitionsMap ++ that.definitionsMap)

    def get(key: Metadata.Key): Option[mode.Callable] = definitionsMap.get(key)
    def get(name: String, version: Option[String] = None): Option[dev.vgerasimov.grafex.core.definitions.mode.Callable] = {
      version match {
        case Some(v) => get(Metadata.Key(name, v))
        case None =>
          definitionsMap.toList
            .filter(p => p._1.name == name)
            .sortBy(p => p._1.version)
            .reverse
            .headOption
            .map(_._2)
      }
    }
  }

  /** Factory for [[Metadata]]. */
  private object Metadata {
    final case class Key(name: String, version: String)

    /** Creates an empty metadata. */
    private def apply(): Metadata = new Metadata(Map())

    /** Creates metadata with one single definition. */
    def apply(modeDefinition: mode.Callable): Metadata = {
      new Metadata(
        Map(
          Key(
            modeDefinition.id.name,
            modeDefinition.id.version
          ) -> modeDefinition
        )
      )
    }
  }
}
