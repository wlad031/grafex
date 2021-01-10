package com.grafex.core
package definition

import com.grafex.core.mode.Mode
import com.grafex.core.mode.Mode.Call

object mode {
  final case class Name(name: String)
  final case class Version(version: String)

  final case class Id(name: String, version: String)

  sealed trait Callable extends Definition {
    def id: Id
    def isLatest: Boolean = false
  }

  sealed trait Definition {

    def suitsFor(call: Mode.Call, inputType: InputType, outputType: OutputType): Boolean = {
      suitsFor(call) && doesSupport(inputType) && doesSupport(outputType)
    }

    def suitsFor(call: Mode.Call): Boolean
    def doesSupport(inputType: InputType): Boolean
    def doesSupport(outputType: OutputType): Boolean
  }

  object Definition {

    def apply(
      name: String,
      version: String,
      inputTypes: Set[InputType],
      outputTypes: Set[OutputType],
      actionDefinitions: Set[action.Definition],
      description: String = null
    ): BasicDefinition = {
      BasicDefinition(
        Id(name, version),
        Option(description),
        inputTypes,
        outputTypes,
        actionDefinitions
      )
    }
  }

  final case class BasicDefinition(
    override val id: Id,
    description: Option[String],
    inputTypes: Set[InputType],
    outputTypes: Set[OutputType],
    actions: Set[action.Definition],
    override val isLatest: Boolean = false
  ) extends Definition
      with Callable {

    override def suitsFor(call: Mode.Call): Boolean = call match {
      case Call.Full(modeId, _)     => this.id == modeId
      case Call.Latest(modeName, _) => isLatest && this.id.name == modeName
    }

    override def doesSupport(inputType: InputType): Boolean = inputTypes.contains(inputType)
    override def doesSupport(outputType: OutputType): Boolean = outputTypes.contains(outputType)

    def toLatest: BasicDefinition = this.copy(isLatest = true)
  }

  final case class OrElseDefinition(
    left: Definition,
    right: Definition
  ) extends Definition {
    private[this] def or[A](f: (Definition, A) => Boolean)(a: A): Boolean = f(left, a) || f(right, a)

    override def suitsFor(call: Mode.Call): Boolean = or[Mode.Call](_.suitsFor(_))(call)
    override def doesSupport(inputType: InputType): Boolean = or[InputType](_.doesSupport(_))(inputType)
    override def doesSupport(outputType: OutputType): Boolean = or[OutputType](_.doesSupport(_))(outputType)
  }

  final case class AndThenDefinition(
    first: Definition,
    next: Definition
  ) extends Definition {
    // FIXME: impossible to implement
    override def suitsFor(call: Mode.Call): Boolean = ???
    override def doesSupport(inputType: InputType): Boolean = first.doesSupport(inputType)
    override def doesSupport(outputType: OutputType): Boolean = next.doesSupport(outputType)
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
    override def doesSupport(inputType: InputType): Boolean = definition.doesSupport(inputType)
    override def doesSupport(outputType: OutputType): Boolean = definition.doesSupport(outputType)

    def toLatest: AliasDefinition = this.copy(isLatest = true)(definition)
  }
}
