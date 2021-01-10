package com.grafex.modes.describe

import cats.data.EitherT
import cats.effect.Sync
import com.grafex.core._
import com.grafex.core.conversion._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.definition.annotations.actionId
import com.grafex.core.mode.Mode.{MFunction, ModeInitializationError }
import com.grafex.core.mode.{Mode, ModeError}
import com.grafex.core.syntax.ActionRequestOps
import io.circe.generic.auto._
import com.grafex.core.definition.implicits.all._

class DescribeMode[F[_] : Sync : RunContext] private (
  otherDefinitions: => Seq[definition.mode.Callable],
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
  val definition = com.grafex.core.definition.mode.Definition(
    name = "describe",
    version = "1",
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      actions.ListModeKeys.definition,
      actions.GetModeDefinition.definition
    )
  )

  def apply[F[_] : Sync : RunContext](
    otherDefinitions: => Seq[com.grafex.core.definition.mode.Callable],
    amILatest: Boolean = true
  ): Either[ModeInitializationError, DescribeMode[F]] = {
    Right(new DescribeMode(otherDefinitions, amILatest))
  }

  sealed trait Request
  sealed trait Response
  sealed trait Error extends ModeError

  case class UnknownModeError(modeName: String) extends Error

  object actions {

    @actionId(name = "list-mode-keys")
    object ListModeKeys {
      val definition = com.grafex.core.definition.action.Definition.derive[ListModeKeys.type, Request, Response]

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

    @actionId("get-def")
    object GetModeDefinition {
      val definition = com.grafex.core.definition.action.Definition.derive[this.type, Request, Response]

      def apply(request: Request)(implicit fullMetadata: Metadata): Either[ModeError, DescribeMode.Response] = {
        val name = request.modeName
        val version = request.modeVersion
        fullMetadata
          .get(name, version)
          .toRight(UnknownMode(name, version))
          .map(_.asInstanceOf[com.grafex.core.definition.mode.BasicDefinition]) // FIXME: unsafe operation
          .map(ModeDefinition.apply)
          .map(GetModeDefinition.Response)
      }

      case class Request(modeName: String, modeVersion: Option[String]) extends DescribeMode.Request
      case class Response(definition: ModeDefinition) extends DescribeMode.Response

      case class UnknownMode(name: String, version: Option[String]) extends ModeError

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
        def apply(md: com.grafex.core.definition.mode.BasicDefinition): ModeDefinition = {
          ModeDefinition(
            md.id.name,
            md.description,
            Version(md.id.version, None), // FIXME: use real version description
            md.inputTypes.map(_.toString).toList,
            md.outputTypes.map(_.toString).toList,
            md.actions
              .map(ad =>
                ActionDefinition(
                  ad.id.name,
                  List() // FIXME
//                  ad.params.map(pd => ParamDefinition(pd.name.toString)).toList
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
    case req if actions.ListModeKeys.definition.suitsFor(req.calls.head.actionId) =>
      req.asActionRequest[actions.ListModeKeys.Request]
    case req if actions.GetModeDefinition.definition.suitsFor(req.calls.head.actionId) =>
      req.asActionRequest[actions.GetModeDefinition.Request]
  }

  /** Encapsulates the map containing mode definitions in convenient for searching way. */
  private class Metadata(val definitionsMap: Map[Metadata.Key, com.grafex.core.definition.mode.Callable]) {

    /** Returns new metadata which contains definitions from this and provided metadata. */
    def ++ (that: Metadata): Metadata = new Metadata(this.definitionsMap ++ that.definitionsMap)

    def keys: Iterable[Metadata.Key] = definitionsMap.keys
    def get(key: Metadata.Key): Option[com.grafex.core.definition.mode.Callable] = definitionsMap.get(key)
    def get(name: String, version: Option[String] = None): Option[com.grafex.core.definition.mode.Callable] = {
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
    case class Key(name: String, version: String)

    /** Creates an empty metadata. */
    private def apply(): Metadata = new Metadata(Map())

    /** Creates metadata with one single definition. */
    def apply(modeDefinition: com.grafex.core.definition.mode.Callable): Metadata = {
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
