package dev.vgerasimov.grafex
package core
package definitions

import shapeless.Annotation
import dev.vgerasimov.grafex.core.Mode.Call
import dev.vgerasimov.grafex.core.conversion.ActionRequestDecoder
import dev.vgerasimov.grafex.core.definitions.annotations.modeId

object mode {
  final case class Name(name: String)
  final case class Version(version: String)

  final case class Id(name: String, version: String)

  sealed trait Callable extends Definition {
    def id: Id
    def isLatest: Boolean = false
  }

  sealed trait Definition {
    def suitsFor(call: Mode.Call): Boolean
  }

  // TODO: try to find out how not to mix definitions and decoders

  /**
    *
    * @param actionDefinition
    * @param actionRequestDecoder
    * @tparam A the action type
    * @tparam AIn the type of action input
    * @tparam AOut the type of action output
    */
  final case class DecodableActionDefinition[A, AIn, AOut](
    actionDefinition: action.Definition[A, AIn, AOut],
    actionRequestDecoder: ActionRequestDecoder[AIn]
  )

  object Definition {

    def instance[M, MIn, MOut](
      actionDefinitions: Set[DecodableActionDefinition[_, _ <: MIn, _ <: MOut]]
    )(
      implicit
      modeIdA: Annotation[modeId, M]
//      descA: Annotation[Option[description], M]
    ): mode.BasicDefinition[M, MIn, MOut] = {
      val id = modeIdA()
      mode.Definition.apply(
        id.name,
        id.version,
        actionDefinitions,
        None //descA().map(_.s)
      )
    }

    def apply[M, MIn, MOut](
      name: String,
      version: String,
      actionDefinitions: Set[DecodableActionDefinition[_, _ <: MIn, _ <: MOut]],
      description: Option[String] = None
    ): BasicDefinition[M, MIn, MOut] = {
      BasicDefinition(
        Id(name, version),
        description,
        actionDefinitions
      )
    }
  }

  /**
    *
    * @param id
    * @param description
    * @param actions
    * @param isLatest
    * @tparam M the mode type
    * @tparam MIn the type of mode input
    * @tparam MOut the type of mode output
    */
  final case class BasicDefinition[M, MIn, MOut](
    override val id: Id,
    description: Option[String],
    actions: Set[DecodableActionDefinition[_, _ <: MIn, _ <: MOut]],
    override val isLatest: Boolean = false
  ) extends Definition
      with Callable {

    override def suitsFor(call: Mode.Call): Boolean = call match {
      case Call.Full(modeId, _)     => this.id == modeId
      case Call.Latest(modeName, _) => isLatest && this.id.name == modeName
    }

    def toLatest: BasicDefinition[M, MIn, MOut] = this.copy(isLatest = true)
  }

  final case class OrElseDefinition(
    left: Definition,
    right: Definition
  ) extends Definition {
    private[this] def or[A](f: (Definition, A) => Boolean)(a: A): Boolean = f(left, a) || f(right, a)

    override def suitsFor(call: Mode.Call): Boolean = or[Mode.Call](_.suitsFor(_))(call)
  }

  final case class AndThenDefinition(
    first: Definition,
    next: Definition
  ) extends Definition {
    // FIXME: impossible to implement
    override def suitsFor(call: Mode.Call): Boolean = ???
  }

  final case class AliasDefinition(
    overriddenId: Id,
    description: Option[String] = None,
    override val isLatest: Boolean = false
  )(
    definition: Definition
  ) extends Definition
      with Callable {

    override val id: Id = overriddenId

    override def suitsFor(call: Mode.Call): Boolean = call match {
      case Call.Full(modeKey, _)    => overriddenId == modeKey
      case Call.Latest(modeName, _) => isLatest && overriddenId.name == modeName
    }
    def toLatest: AliasDefinition = this.copy(isLatest = true)(definition)
  }
}
