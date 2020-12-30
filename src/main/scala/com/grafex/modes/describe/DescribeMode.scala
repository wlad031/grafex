package com.grafex.modes.describe

import cats.data.EitherT
import cats.effect.Sync
import com.grafex.core._
import com.grafex.core.conversion._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.mode.Mode.{ MFunction, ModeInitializationError, Param }
import com.grafex.core.mode.{ Mode, ModeError }
import com.grafex.core.syntax.ActionRequestOps
import io.circe.generic.auto._

class DescribeMode[F[_] : Sync : RunContext] private (
  otherDefinitions: => Seq[Mode.Definition.Callable],
  amILatest: Boolean = true
) extends MFunction[F, DescribeMode.Request, DescribeMode.Response] {
  import DescribeMode.actions

  private[this] lazy val fullMetadata: DescribeMode.Metadata =
    DescribeMode.Metadata(if (amILatest) DescribeMode.definition.toLatest else DescribeMode.definition) ++ otherDefinitions
      .map(DescribeMode.Metadata(_))
      .reduce(_ ++ _)

  override def apply(request: DescribeMode.Request): EitherT[F, ModeError, DescribeMode.Response] = {
    implicit val fm: DescribeMode.Metadata = fullMetadata
    request match {
      case req: actions.ListModeKeys.Request      => EitherT.fromEither(actions.ListModeKeys(req))
      case req: actions.GetModeDefinition.Request => EitherT.fromEither(actions.GetModeDefinition(req))
    }
  }
}

object DescribeMode {
  val definition: Mode.Definition.Basic = Mode.Definition.Basic(
    Mode.Key(Mode.Name("describe"), Mode.Version("1")),
    description = None,
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      actions.ListModeKeys.definition,
      actions.GetModeDefinition.definition
    )
  )

  def apply[F[_] : Sync : RunContext](
    otherDefinitions: => Seq[Mode.Definition.Callable],
    amILatest: Boolean = true
  ): Either[ModeInitializationError, DescribeMode[F]] = {
    Right(new DescribeMode(otherDefinitions, amILatest))
  }

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  case class UnknownModeError(modeName: String) extends Error

  object actions {

    object ListModeKeys {
      val definition: Mode.Action.Definition =
        Mode.Action.Definition(Mode.Action.Key(Mode.Action.Name("list-mode-keys")), None, Set())

      def apply(request: Request)(implicit fullMetadata: Metadata): Either[ModeError, DescribeMode.Response] = Right(
        request.requestMode match {
          case Some(RequestMode.Full()) =>
            ListModeKeys.Response.Full(
              fullMetadata.definitionsMap
                .map({ case (k, d) => ModeKey(k.name.toString, k.version.toString, d.isLatest) })
                .toList
            )
          // Short is default request mode
          case _ =>
            ListModeKeys.Response.Short(
              fullMetadata.definitionsMap
                .map({ case (k, _) => s"${k.name.toString}.${k.version.toString}" })
                .toList
            )
        }
      )

      case class Request(requestMode: Option[RequestMode]) extends DescribeMode.Request

      sealed trait Response extends DescribeMode.Response

      object Response {

        case class Full(modeKeys: List[ModeKey]) extends Response
        object Full {
          implicit val enc: ActionResponseEncoder[Full] = deriveOnlyJsonActionResponseEncoder
        }

        case class Short(modeKeys: List[String]) extends Response
        object Short {
          implicit val enc: ActionResponseEncoder[Short] = deriveOnlyJsonActionResponseEncoder
        }
      }

      sealed trait RequestMode
      object RequestMode {
        case class Full() extends RequestMode
        case class Short() extends RequestMode
      }

      case class ModeKey(name: String, version: String, isVersionLatest: Boolean)

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder
    }

    object GetModeDefinition {
      val definition: Mode.Action.Definition = Mode.Action.Definition(
        Mode.Action.Key(Mode.Action.Name("get-mode-def")),
        None,
        Set(
          Param("modeName"),
          Param("modeVersion")
        )
      )

      def apply(request: Request)(implicit fullMetadata: Metadata): Either[ModeError, DescribeMode.Response] = {
        val name = Mode.Name(request.modeName)
        val version = request.modeVersion.map(Mode.Version.apply)
        fullMetadata
          .get(name, version)
          .toRight(UnknownMode(name, version))
          .map(_.asInstanceOf[Mode.Definition.Basic]) // FIXME: unsafe operation
          .map(ModeDefinition.apply)
          .map(GetModeDefinition.Response)
      }

      case class Request(modeName: String, modeVersion: Option[String]) extends DescribeMode.Request
      case class Response(definition: ModeDefinition) extends DescribeMode.Response

      case class UnknownMode(name: Mode.Name, version: Option[Mode.Version]) extends ModeError

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder

      case class ModeDefinition(
        name: String,
        description: Option[String],
        version: Version,
        supportedInputTypes: List[String],
        supportedOutputTypes: List[String],
        actions: List[ActionDefinition]
      )

      object ModeDefinition {
        def apply(md: Mode.Definition.Basic): ModeDefinition = {
          ModeDefinition(
            md.modeKey.name.toString,
            md.description,
            Version(md.modeKey.version.toString, None), // FIXME: use real version description
            md.supportedInputTypes.map(_.toString).toList,
            md.supportedOutputTypes.map(_.toString).toList,
            md.actions
              .map(ad =>
                ActionDefinition(
                  ad.actionKey.name.toString,
                  ad.params.map(pd => ParamDefinition(pd.name.toString)).toList
                )
              )
              .toList
          )
        }
      }

      case class Version(version: String, description: Option[String])
      case class ActionDefinition(name: String, params: List[ParamDefinition])
      case class ParamDefinition(name: String)
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instance {
    case req if actions.ListModeKeys.definition.suitsFor(req.calls.head.actionKey) =>
      req.asActionRequest[actions.ListModeKeys.Request]
    case req if actions.GetModeDefinition.definition.suitsFor(req.calls.head.actionKey) =>
      req.asActionRequest[actions.GetModeDefinition.Request]
  }

  /** Encapsulates the map containing mode definitions in convenient for searching way. */
  private class Metadata(val definitionsMap: Map[Metadata.Key, Mode.Definition.Callable]) {

    /** Returns new metadata which contains definitions from this and provided metadata. */
    def ++ (that: Metadata): Metadata = new Metadata(this.definitionsMap ++ that.definitionsMap)

    def keys: Iterable[Metadata.Key] = definitionsMap.keys
    def get(key: Metadata.Key): Option[Mode.Definition.Callable] = definitionsMap.get(key)
    def get(name: Mode.Name, version: Option[Mode.Version] = None): Option[Mode.Definition.Callable] = {
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
    case class Key(name: Mode.Name, version: Mode.Version)

    /** Creates an empty metadata. */
    private def apply(): Metadata = new Metadata(Map())

    /** Creates metadata with one single definition. */
    def apply(modeDefinition: Mode.Definition.Callable): Metadata = {
      new Metadata(
        Map(
          Key(
            modeDefinition.modeKey.name,
            modeDefinition.modeKey.version
          ) -> modeDefinition
        )
      )
    }
  }
}
