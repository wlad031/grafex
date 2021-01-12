package com.grafex.core

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.{ ConcurrentEffect, Sync }
import cats.instances.option._
import cats.syntax.either._
import cats.syntax.semigroupk._
import com.grafex.core.ModeError.InvalidRequest
import com.grafex.core.conversion.{ ModeRequestDecoder, ModeResponseEncoder }
import com.grafex.core.definitions._

/** Represents any possible "mode".
  *
  * Basically, "mode" is just an [[Mode.MFunction]] with input type [[ModeRequest]] and output type [[ModeResponse]].
  * Also, it provides a [[mode.Definition]] of itself.
  *
  * Such modes could be easily composed in different ways:
  *   1. `orElse` composition;
  *   1. `andThen` composition;
  *   1. aliasing.
  *
  * @tparam F the type of the effect
  */
sealed abstract class Mode[F[_] : Sync : RunContext] extends Mode.MFunction[F, ModeRequest, ModeResponse] {
  import Mode._

  /** The definition of the mode. */
  def definition: mode.Definition

  /** `orElse` composition checks whether request suits better for this of that mode and then delegates request to it.
    *
    * @group Mode constructing
    *
    * @param that another mode to be composed
    * @return new composed mode
    */
  def orElse(that: Mode[F]): Mode[F] = new OrElse[F](left = this, right = that)

  val rarc = resAndReqCombiner(definition)

  /** `andThen` composition allows to chain modes in the following way:
    * {{{
    *                                     ┌────────────────────────────────────┐
    *      ┌───────┐  ┌──────┐  ┌────────┐┘ ┌───────────┐  ┌──────┐  ┌────────┐└>┌───────────┐  ┌────────┐
    *      │user's │─>│mode 1│─>│response│─>│combine    │─>│mode 2│─>│response│─>│combine    │  │final   │
    *      │request│  └──────┘  └────────┘┌>│request and│  └──────┘  └────────┘  │both       │─>│response│
    *      └───────┘──────────────────────┘ │response   │                        │responses  │  └────────┘
    *                                       └───────────┘                        └───────────┘
    * }}}
    * @note The chain could be as long as needed.
    * @note Default request+response and response+response combiners parse everything as JSONs
    *       and then merge JSONs using [[io.circe.Json.deepMerge]]
    * @note In general, combiners aren't associative.
    *
    * @group Mode constructing
    *
    * @param other another mode to be chained
    * @return new composed mode
    */
  def andThen(
    other: Mode[F]
  )(
    implicit
    resAndReqCombiner: ModeResponseWithRequestCombiner = rarc,
    resCombiner: ModeResponseWithResponseCombiner = resCombiner
  ): Either[InvalidRequest.ModesNotCombinable, Mode[F]] = {
    if (!this.definition.doesSupport(OutputType.Json) && other.definition.doesSupport(InputType.Json) ||
        !this.definition.doesSupport(OutputType.Json) && other.definition.doesSupport(OutputType.Json)) {
      InvalidRequest.ModesNotCombinable(this.definition, other.definition).asLeft
    } else {
      new AndThen[F](first = this, next = other).asRight
    }
  }

  // TODO: improve this doc
  /** `alias` is not exactly a composition, but allows to override a definition for the given mode.
    *
    * @group Mode constructing
    * */
  def alias(
    callOverrides: CallOverrides,
    definitionCreator: mode.Definition => mode.AliasDefinition
  ): Mode[F] =
    new Alias[F](modeToAlias = this)(callOverrides, definitionCreator)
}

/** Factory for [[Mode]] instances.
  *
  * Also, contains [[Mode]] implementations and type classes needed for them.
  */
object Mode {

  /** Represents any function from some A to [[scala.Either]] B or [[ModeError]] wrapped in some effect F.
    *
    * @tparam F the type of the effect
    * @tparam A the type of function input
    * @tparam B the type of function output
    */
  trait MFunction[F[_], A, B] extends (A => EitherT[F, ModeError, B]) {
    override def apply(request: A): EitherT[F, ModeError, B]
  }

  sealed trait DynamicMFunction[F[_], A, B] extends MFunction[F, A, B]

  object DynamicMFunction {
    def web[F[_] : ConcurrentEffect : RunContext, A, B](config: Web.Config): DynamicMFunction[F, A, B] = {
      new Web[F, A, B](config)
    }

    private final class Web[F[_] : ConcurrentEffect : RunContext, A, B](
      config: Web.Config
    ) extends DynamicMFunction[F, A, B] {
      import org.http4s.client.blaze._

      import scala.concurrent.ExecutionContext.global

      override def apply(request: A): EitherT[F, ModeError, B] = {
        EitherT.right(BlazeClientBuilder[F](global).resource.use[F, B] { client =>
//          val value: F[B] = client.expect[B](config.uri)
//          value
          ???
        })
      }
    }

    object Web {
      case class Config(uri: String)
    }
  }

  /** Summoner for basic mode using it's [[definitions.mode.Definition]] and [[MFunction]].
    *
    * @param definition the mode's definition
    * @param f the function that does mode's work
    * @tparam F the type of the effect
    * @tparam A the type of mode input
    * @tparam B the type of mode output
    * @return instantiated basic mode
    */
  def instance[F[_] : Sync : RunContext, A, B](definition: mode.BasicDefinition, f: MFunction[F, A, B])(
    implicit
    modeRequestDecoder: ModeRequestDecoder[A],
    modeResponseEncoder: ModeResponseEncoder[B]
  ): Mode[F] =
    new Basic[F, A, B](f, definition)

  def instance[F[_] : Sync : RunContext, A, B](
    definition: mode.BasicDefinition,
    fe: Either[ModeInitializationError, MFunction[F, A, B]]
  )(
    implicit
    modeRequestDecoder: ModeRequestDecoder[A],
    modeResponseEncoder: ModeResponseEncoder[B]
  ): Either[ModeInitializationError, Mode[F]] = fe match {
    case Left(error) => Left(error)
    case Right(f)    => Right(instance[F, A, B](definition, f))
  }

  /** Instantiates the dynamic mode. */
  def dynamic[F[_] : Sync : RunContext, A : ModeRequestDecoder, B : ModeResponseEncoder](
    f: MFunction[F, A, B]
  ): Mode[F] = ???

  def dyn[F[_] : Sync : RunContext, A, B](f: MFunction[F, A, B]): DynamicMFunction[F, A, B] = { ??? }

  /** Represents how mode and some of it's actions can be found and called. */
  sealed trait Call {

    /** Returns the [[definitions.action.Id]] of the call, which should be presented in any implementation of [[Call]]. */
    def actionId: definitions.action.Id
  }

  /** Contains implementations of [[Call]]. */
  object Call {

    /** Represents the case when mode called using it's name, version and action name. */
    final case class Full(
      modeId: definitions.mode.Id,
      override val actionId: definitions.action.Id
    ) extends Call

    /** Represents the case when mode called using it's name and action name.
      * In this case the version which is marked as latest will be used.
      */
    final case class Latest(
      modeName: String,
      override val actionId: definitions.action.Id
    ) extends Call

    /** Creates new mode full call.
      *
      * @note It's just an alias for mode full call main constructor, added just for simplifying the creation of call.
      */
    def apply(modeId: definitions.mode.Id, actionId: definitions.action.Id): Mode.Call.Full = Full(modeId, actionId)

    /** Creates new mode latest call.
      *
      * @note It's just an alias for mode latest call main constructor, added just for simplifying the creation of call.
      */
    def apply(modeName: String, actionId: definitions.action.Id): Mode.Call.Latest = Latest(modeName, actionId)
  }

  type CallOverrides = Map[NonEmptyList[Call], NonEmptyList[Call]]

  // region Mode implementations

  private class Basic[F[_] : Sync : RunContext, A, B](
    f: MFunction[F, A, B],
    override val definition: mode.BasicDefinition
  )(
    implicit
    modeRequestDecoder: ModeRequestDecoder[A],
    modeResponseEncoder: ModeResponseEncoder[B]
  ) extends Mode[F] {

    private[this] def validateRequest(request: ModeRequest): Option[ModeError] =
      checkRequestTypeSupport(definition, request) <+>
      Option.when(
        request.calls
          .map(definition.suitsFor)
          .filterNot(x => x)
          .nonEmpty
      )(InvalidRequest.WrongMode(definition, request): ModeError)

    override def apply(request: ModeRequest): EitherT[F, ModeError, ModeResponse] =
      validateRequest(request) match {
        case Some(error) => error.asLeft.toEitherT[F]
        case None =>
          if (request.calls.size > 1)
            (this andThen this)
              .toEitherT[F]
              .flatMap(_.apply(request))
          else
            for {
              modeRequest      <- modeRequestDecoder.apply(request).toEitherT[F]
              modeResponse     <- f(modeRequest)
              abstractResponse <- modeResponseEncoder.apply(modeResponse)(request).toEitherT[F]
            } yield abstractResponse
      }
  }

  private class OrElse[F[_] : Sync : RunContext](left: Mode[F], right: Mode[F]) extends Mode[F] {

    override val definition: mode.OrElseDefinition =
      mode.OrElseDefinition(left.definition, right.definition)

    private[this] def validateRequestAndGetMode(request: ModeRequest): Either[ModeError, Mode[F]] =
      checkRequestTypeSupport(this.definition, request) match {
        case Some(error: ModeError) => Left(error)
        case None =>
          if (request.calls.size == 1) {
            if (definition.left.suitsFor(request.calls.head)) Right(left)
            else if (definition.right.suitsFor(request.calls.head)) Right(right)
            else Left(InvalidRequest.WrongMode(definition, request))
          } else if (request.calls
                       .map(definition.suitsFor)
                       .filterNot(x => x) // TODO: one of my best lines
                       .isEmpty) {
            this andThen this
          } else {
            Left(InvalidRequest.WrongMode(definition, request))
          }
      }

    override def apply(request: ModeRequest): EitherT[F, ModeError, ModeResponse] =
      validateRequestAndGetMode(request) match {
        case Left(error) => error.asLeft.toEitherT[F]
        case Right(mode) => mode(request)
      }
  }

  type ModeResponseWithRequestCombiner = (ModeResponse, ModeRequest) => Either[ModeError, ModeRequest]
  type ModeResponseWithResponseCombiner = (ModeResponse, ModeResponse) => Either[ModeError, ModeResponse]

  private def resAndReqCombiner(definition: mode.Definition): ModeResponseWithRequestCombiner = (res, req) => {
    req.calls match {
      case NonEmptyList(_, Nil) => InvalidRequest.NotEnoughCalls(definition, req).asLeft
      case NonEmptyList(_, x :: xs) =>
        (res, req) match {
          case (ModeResponse.Json(resBody), ModeRequest.Json(_, _, reqBody)) =>
            ModeRequest.Json(NonEmptyList(x, xs), OutputType.Json, resBody deepMerge reqBody).asRight
        }
    }
  }

  private def resCombiner: ModeResponseWithResponseCombiner = (res, req) => {
    (res, req) match {
      case (ModeResponse.Json(res1Body), ModeResponse.Json(res2Body)) =>
        ModeResponse.Json(res1Body deepMerge res2Body).asRight
    }
  }

  private class AndThen[F[_] : Sync : RunContext](
    first: Mode[F],
    next: Mode[F]
  )(
    implicit
    resAndReqCombiner: ModeResponseWithRequestCombiner,
    resCombiner: ModeResponseWithResponseCombiner
  ) extends Mode[F] {

    override def definition: mode.Definition = mode.AndThenDefinition(first.definition, next.definition)

    private[this] def validateAndPrepareRequest(
      request: ModeRequest
    ): Either[ModeError, ((Mode[F], ModeRequest), (Mode[F], ModeRequest))] = {
      def ifFirstNotAndThen(
        f: Mode[F],
        n: Mode[F]
      ): Either[ModeError, ((Mode[F], ModeRequest), (Mode[F], ModeRequest))] = {
        // FIXME: unsafe operation .get
        val firstRequest = request.dropTail(OutputType.Json)
        val firstCall = firstRequest.firstCall
        val maybeNextRequest = request.dropFirst()
        val maybeNextCall = maybeNextRequest.map(_.calls.head)
        val error =
          checkRequestTypeSupport(f.definition, firstRequest) <+>
          Option.when(!f.definition.suitsFor(firstCall))(
            InvalidRequest.WrongMode(definition, request): ModeError
          ) <+>
          (maybeNextCall match {
            case Some(call) =>
              Option.when(!n.definition.suitsFor(call))(
                InvalidRequest.WrongMode(definition, request): ModeError
              )
            case None => Some(InvalidRequest.NotEnoughCalls(definition, request))
          })
        error match {
          case Some(error) => Left(error)
          case None =>
            Right(
              (f, firstRequest),
              (n, maybeNextRequest.get) // FIXME: unsafe operation .get
            )
        }
      }

      (first, next) match {
        case (f: Basic[F, _, _], n) => ifFirstNotAndThen(f, n)
        case (f: OrElse[F], n)      => ifFirstNotAndThen(f, n)
        case (f: Dynamic[F], n)     => ifFirstNotAndThen(f, n)
        case (f: Alias[F], n)       => ???
        case (f: AndThen[F], n)     => ???
      }
    }

    override def apply(request: ModeRequest): EitherT[F, ModeError, ModeResponse] =
      validateAndPrepareRequest(request) match {
        case Left(error) => error.asLeft.toEitherT[F]
        case Right(((m1, r1), (m2, r2))) =>
          for {
            firstRes  <- m1(r1)
            newReq    <- resAndReqCombiner(firstRes, request).toEitherT[F]
            secondRes <- m2(newReq)
            finalRes  <- resCombiner(firstRes, secondRes).toEitherT[F]
          } yield finalRes
      }
  }

  sealed abstract class Dynamic[F[_] : Sync : RunContext] extends Mode[F]

  object Dynamic {

    class Web[F[_] : Sync : RunContext, A, B](config: Web.Config) extends Dynamic[F] {
      override def definition: mode.Definition = ???
      override def apply(request: ModeRequest): EitherT[F, ModeError, ModeResponse] = ???
    }

    object Web {
      case class Config(modeName: String, host: String)
    }
  }

  private class Alias[F[_] : Sync : RunContext](
    modeToAlias: Mode[F]
  )(callOverrides: CallOverrides, definitionCreator: definitions.mode.Definition => definitions.mode.AliasDefinition)
      extends Mode[F] {
    override val definition: mode.Definition = definitionCreator(modeToAlias.definition)

    override def apply(request: ModeRequest): EitherT[F, ModeError, ModeResponse] = ???
  }

  private def checkRequestTypeSupport(
    definition: mode.Definition,
    request: ModeRequest
  ): Option[ModeError] =
    Option.when(!definition.doesSupport(request.inputType))(
      InvalidRequest.UnsupportedInputType(request): ModeError
    ) <+>
    Option.when(!definition.doesSupport(request.outputType))(
      InvalidRequest.UnsupportedOutputType(request.outputType): ModeError
    )

  // endregion

  // region Errors

  sealed trait ModeInitializationError extends GrafexError
  object ModeInitializationError {
    final case class NeededCallUnsupported() extends ModeInitializationError
  }

  // endregion

  // region Definitions

//  case class DependencyCreationError(mode: Mode[_], definition: Definition.Callable) extends GrafexError

//  final case class Key(name: Name, version: Version)
//
//  sealed trait Definition {
//    def suitsFor(call: Mode.Call, inputType: InputType, outputType: OutputType): Boolean = {
//      suitsFor(call) && doesSupport(inputType) && doesSupport(outputType)
//    }
//
//    def suitsFor(call: Mode.Call): Boolean
//    def doesSupport(inputType: InputType): Boolean
//    def doesSupport(outputType: OutputType): Boolean
//  }
//
//  object Definition {
//
//    sealed trait Callable extends Definition {
//      def modeKey: Mode.Key
//      def isLatest: Boolean = false
//    }
//
//    case class Basic(
//      override val modeKey: Mode.Key,
//      description: Option[String] = None,
//      supportedInputTypes: Set[InputType],
//      supportedOutputTypes: Set[OutputType],
//      actions: Set[Action.Definition],
//      override val isLatest: Boolean = false
//    ) extends Definition
//        with Callable {
//      private[this] def suitsOneAction(actionKey: Action.Key): Boolean = // TODO: implement
//        true //actions.map(_.suitsFor(actionKey)).size == 1
//
//      override def suitsFor(call: Mode.Call): Boolean = call match {
//        case Call.Full(modeKey, actionKey)    => this.modeKey == modeKey && suitsOneAction(actionKey)
//        case Call.Latest(modeName, actionKey) => isLatest && this.modeKey.name == modeName && suitsOneAction(actionKey)
//      }
//
//      override def doesSupport(inputType: InputType): Boolean = supportedInputTypes.contains(inputType)
//      override def doesSupport(outputType: OutputType): Boolean = supportedOutputTypes.contains(outputType)
//
//      def toLatest: Basic = this.copy(isLatest = true)
//    }
//
//    case class OrElse(
//      left: Definition,
//      right: Definition
//    ) extends Definition {
//      private[this] def or[A](f: (Definition, A) => Boolean)(a: A): Boolean = f(left, a) || f(right, a)
//
//      override def suitsFor(call: Mode.Call): Boolean = or[Mode.Call](_.suitsFor(_))(call)
//      override def doesSupport(inputType: InputType): Boolean = or[InputType](_.doesSupport(_))(inputType)
//      override def doesSupport(outputType: OutputType): Boolean = or[OutputType](_.doesSupport(_))(outputType)
//    }
//
//    case class AndThen(
//      first: Definition,
//      next: Definition
//    ) extends Definition {
//      // FIXME: impossible to implement
//      override def suitsFor(call: Mode.Call): Boolean = ???
//      override def doesSupport(inputType: InputType): Boolean = first.doesSupport(inputType)
//      override def doesSupport(outputType: OutputType): Boolean = next.doesSupport(outputType)
//    }
//
//    case class Alias(
//      overriddenModeKey: Mode.Key,
//      description: Option[String] = None,
//      override val isLatest: Boolean = false
//    )(
//      definition: Definition
//    ) extends Definition
//        with Callable {
//
//      override val modeKey: Mode.Key = overriddenModeKey
//
//      override def suitsFor(call: Mode.Call): Boolean = call match {
//        case Call.Full(modeKey, _)    => overriddenModeKey == modeKey
//        case Call.Latest(modeName, _) => isLatest && overriddenModeKey.name == modeName
//      }
//      override def doesSupport(inputType: InputType): Boolean = definition.doesSupport(inputType)
//      override def doesSupport(outputType: OutputType): Boolean = definition.doesSupport(outputType)
//
//      def toLatest: Alias = this.copy(isLatest = true)(definition)
//    }
//  }
//
//  case class Name(name: String) {
//    override def toString: String = name
//  }
//
//  object Name {
//    def orElse(left: Name, right: Name): Name = Name(s"${left.name}|${right.name}")
//    def andThen(first: Name, next: Name): Name = Name(s"${first.name}>${next.name}")
//  }
//
//  final case class Version(id: String) {
//    override def toString: String = id
//  }
//
//  object Version {
//
//    implicit val versionOrdering: Ordering[Version] = (x: Version, y: Version) => {
//      val xs: List[String] = x.id.split(".", 3).toList
//      val ys: List[String] = y.id.split(".", 3).toList
//      xs.zipAll(ys, "0", "0")
//        .map(p => p._1.compareTo(p._2))
//        .find(_ != 0)
//        .getOrElse(0)
//    }
//  }
//
//  // TODO: move out from Mode object
//  object Action {
//
//    final case class Key(name: Name)
//
//    case class Name(name: String) {
//      override def toString: String = name
//    }
//
//    case class Definition(
//      actionKey: Action.Key,
//      description: Option[String] = None,
//      params: Set[Param.Definition]
//    ) {
//      def suitsFor(actionKey: Action.Key): Boolean = this.actionKey == actionKey
//    }
//  }
//
//  // TODO: move out from Mode object
//  object Param {
//    case class Name(name: String) {
//      override def toString: String = name
//    }
//
//    object Name {
//      def fromString(name: String) = new Name(name)
//    }
//
//    case class Definition(name: Name, description: Option[String] = None)
//
//    // TODO: move under Definition object
//    def apply(name: String, description: Option[String] = None): Definition =
//      Definition(Name.fromString(name), description)
//  }

  // endregion
}
