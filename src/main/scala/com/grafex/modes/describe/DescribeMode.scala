package com.grafex.modes.describe

import cats.data.EitherT
import cats.effect.Sync
import com.grafex.core._
import com.grafex.core.conversion._
import com.grafex.core.conversion.semiauto._
import com.grafex.core.definitions.annotations.actionId
import com.grafex.core.definitions.generic.auto._
import com.grafex.core.definitions.syntax.ActionDefinitionOps
import com.grafex.core.definitions.{ action, mode }
import com.grafex.core.modeFoo.Mode.{ MFunction, ModeInitializationError }
import com.grafex.core.modeFoo.ModeError
import com.grafex.modes.describe.DescribeMode.actions.{ GetModeDefinitionAction, ListModeKeysAction }
import io.circe.generic.auto._

class DescribeMode[F[_] : Sync : RunContext] private (
  otherDefinitions: => Seq[definitions.mode.Callable],
  amILatest: Boolean = true
) extends MFunction[F, DescribeMode.Request, DescribeMode.Response] {

  private[this] lazy val fullMetadata: DescribeMode.Metadata =
    DescribeMode.Metadata(if (amILatest) DescribeMode.definition.toLatest else DescribeMode.definition) ++ otherDefinitions
      .map(DescribeMode.Metadata(_))
      .reduce(_ ++ _)

  override def apply(request: DescribeMode.Request): EitherT[F, ModeError, DescribeMode.Response] = {
    implicit val fm: DescribeMode.Metadata = fullMetadata
    request match {
      case req: ListModeKeysAction.Request      => EitherT.fromEither(ListModeKeysAction(req))
      case req: GetModeDefinitionAction.Request => EitherT.fromEither(GetModeDefinitionAction(req))
    }
  }
}

object DescribeMode {
  implicit val definition: mode.BasicDefinition = mode.Definition(
    name = "describe",
    version = "1",
    Set(InputType.Json),
    Set(OutputType.Json),
    Set(
      action.Definition
        .instance[ListModeKeysAction.type, ListModeKeysAction.Request, ListModeKeysAction.Response]
        .asDecodable,
      action.Definition
        .instance[GetModeDefinitionAction.type, GetModeDefinitionAction.Request, GetModeDefinitionAction.Response]
        .asDecodable
    )
  )

  def apply[F[_] : Sync : RunContext](
    otherDefinitions: => Seq[mode.Callable],
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
    object ListModeKeysAction {
      def apply(request: Request)(implicit fullMetadata: Metadata): Either[ModeError, DescribeMode.Response] = Right(
        request.requestMode match {
          case Some(RequestMode.Full()) =>
            ListModeKeysAction.Response.Full(
              fullMetadata.definitionsMap
                .map({ case (k, d) => ModeKey(k.name.toString, k.version.toString, d.isLatest) })
                .toList
            )
          // Short is default request mode
          case _ =>
            ListModeKeysAction.Response.Short(
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
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]
    }

    @actionId("get-def")
    object GetModeDefinitionAction {
      def apply(request: Request)(implicit fullMetadata: Metadata): Either[ModeError, DescribeMode.Response] = {
        val name = request.modeName
        val version = request.modeVersion
        fullMetadata
          .get(name, version)
          .toRight(UnknownMode(name, version))
          .map(_.asInstanceOf[mode.BasicDefinition]) // FIXME: unsafe operation
          .map(ModeDefinition.apply)
          .map(GetModeDefinitionAction.Response)
      }

      case class Request(modeName: String, modeVersion: Option[String]) extends DescribeMode.Request
      case class Response(definition: ModeDefinition) extends DescribeMode.Response

      case class UnknownMode(name: String, version: Option[String]) extends ModeError

      implicit val enc: ActionResponseEncoder[Response] = deriveOnlyJsonActionResponseEncoder
      implicit val dec: ActionRequestDecoder[Request] = deriveOnlyJsonActionRequestDecoder[Request]

      case class ModeDefinition(
        name: String,
        description: Option[String],
        version: String,
        supportedInputTypes: List[String],
        supportedOutputTypes: List[String],
        actions: List[ActionDefinition]
      )

      object ModeDefinition {
        def apply(md: mode.BasicDefinition): ModeDefinition = {
          ModeDefinition(
            md.id.name,
            md.description,
            md.id.version,
            md.inputTypes.map(_.toString).toList,
            md.outputTypes.map(_.toString).toList,
            md.actions
              .map(ad =>
                ActionDefinition(
                  ad.actionDefinition.id.name,
                  ad.actionDefinition.description,
                  List() // FIXME
//                  ad.params.map(pd => ParamDefinition(pd.name.toString)).toList
                )
              )
              .toList
          )
        }
      }

      case class ActionDefinition(name: String, description: Option[String], params: List[ParamDefinition])
      case class ParamDefinition(name: String)
    }
  }

  implicit val enc: ModeResponseEncoder[Response] = deriveModeResponseEncoder
  implicit val dec: ModeRequestDecoder[Request] = ModeRequestDecoder.instanceF

  /** Encapsulates the map containing mode definitions in convenient for searching way. */
  private class Metadata(val definitionsMap: Map[Metadata.Key, com.grafex.core.definitions.mode.Callable]) {

    /** Returns new metadata which contains definitions from this and provided metadata. */
    def ++ (that: Metadata): Metadata = new Metadata(this.definitionsMap ++ that.definitionsMap)

    def keys: Iterable[Metadata.Key] = definitionsMap.keys
    def get(key: Metadata.Key): Option[mode.Callable] = definitionsMap.get(key)
    def get(name: String, version: Option[String] = None): Option[com.grafex.core.definitions.mode.Callable] = {
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
