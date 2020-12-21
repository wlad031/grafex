package com.grafex.core

import cats.data.{ EitherT, NonEmptyList }
import cats.effect.{ ConcurrentEffect, Sync }
import cats.instances.either._
import cats.instances.option._
import cats.syntax.bifunctor._
import cats.syntax.semigroupk._
import com.grafex.core.Mode._
import com.grafex.core.conversion.{ ModeRequestDecoder, ModeResponseEncoder }
import io.circe.parser.parse

/** Represents any possible "mode".
  *
  * Basically, "mode" is just an [[Mode.MFunction]] with input type [[Mode.Request]] and output type [[Mode.Response]].
  * Also, it provides a [[Mode.Definition]] of itself.
  *
  * Such modes could be easily composed in different ways:
  *   1. `orElse` composition;
  *   1. `andThen` composition;
  *   1. aliasing.
  *
  * @tparam F the type of the effect
  */
sealed abstract class Mode[F[_] : Sync : RunContext] extends MFunction[F, Request, Response] {

  /** The definition of the mode. */
  def definition: Definition

  /** `orElse` composition checks whether request suits better for this of that mode and then delegates request to it.
    *
    * @group Mode constructing
    *
    * @param that another mode to be composed
    * @return new composed mode
    */
  def orElse(that: Mode[F]): Mode[F] = new OrElse[F](left = this, right = that)

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
    resAndReqCombiner: (Mode.Response, Mode.Request) => Either[ModeError, String] = (res, req) => {
      for {
        json1 <- parse(res.body).leftMap(_ => IllegalModeState())
        json2 <- parse(req.body).leftMap(_ => IllegalModeState())
      } yield json1.deepMerge(json2).noSpaces
    },
    resCombiner: (Mode.Response, Mode.Response) => Either[ModeError, String] = (res, req) => {
      for {
        json1 <- parse(res.body).leftMap(_ => IllegalModeState())
        json2 <- parse(req.body).leftMap(_ => IllegalModeState())
      } yield json1.deepMerge(json2).spaces2
    }
  ): Either[ModesNotCombinable, Mode[F]] = {
    if (!this.definition.doesSupport(OutputType.Json) && other.definition.doesSupport(InputType.Json) ||
        !this.definition.doesSupport(OutputType.Json) && other.definition.doesSupport(OutputType.Json)) {
      Left(ModesNotCombinable(this.definition, other.definition))
    } else {
      Right(new AndThen[F](first = this, next = other))
    }
  }

  // TODO: improve this doc
  /** `alias` is not exactly a composition, but allows to override a definition for the given mode.
    *
    * @group Mode constructing
    * */
  def alias(callOverrides: CallOverrides, definitionCreator: Definition => Definition.Alias): Mode[F] =
    new Alias[F](mode = this)(callOverrides, definitionCreator)
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

  /** Summoner for basic mode using it's [[Definition]] and [[MFunction]].
    *
    * @param definition the mode's definition
    * @param f the function that does mode's work
    * @tparam F the type of the effect
    * @tparam A the type of mode input
    * @tparam B the type of mode output
    * @return instantiated basic mode
    */
  def instance[F[_] : Sync : RunContext, A, B](
    definition: Definition.Basic
  )(f: MFunction[F, A, B])(
    implicit
    modeRequestDecoder: ModeRequestDecoder[A],
    modeResponseEncoder: ModeResponseEncoder[B]
  ): Mode[F] =
    new Basic[F, A, B](f, definition)

  /** Instantiates the dynamic mode. */
  def dynamic[F[_] : Sync : RunContext, A : ModeRequestDecoder, B : ModeResponseEncoder](
    f: MFunction[F, A, B]
  ): Mode[F] = ???

  def dyn[F[_] : Sync : RunContext, A, B](f: MFunction[F, A, B]): DynamicMFunction[F, A, B] = { ??? }

  /** Mode's "request" data type.
    *
    * @param calls the non empty list of mode calls
    * @param body the body of the request
    * @param inputType the type of the input data
    * @param outputType requested type of output data
    */
  case class Request(
    calls: NonEmptyList[Call],
    body: String,
    inputType: InputType,
    outputType: OutputType
  ) {

    def dropTail(newOutputType: OutputType = outputType): Request =
      copy(calls = NonEmptyList(calls.head, Nil), outputType = newOutputType)

    def dropFirst(newBody: String = this.body): Option[Request] =
      calls match {
        case NonEmptyList(_, Nil)     => None
        case NonEmptyList(_, x :: xs) => Some(copy(calls = NonEmptyList(x, xs), body = newBody))
      }

    def asSingle: SingleCallRequest = SingleCallRequest(
      calls.head,
      body,
      inputType,
      outputType
    )
  }

  // TODO: find out how to make it better
  case class SingleCallRequest(
    call: Call,
    body: String,
    inputType: InputType,
    outputType: OutputType
  )

  /** Factory for various [[Request]] objects. */
  object Request {

    def web(call: Call, body: String): Request =
      Request(NonEmptyList(call, Nil), body, InputType.Json, OutputType.Json)
  }

  /** Mode's "response" data type.
    *
    * @param body the body of the response
    */
  case class Response(body: String)

  /** Represents how mode and some of it's actions can be found and called. */
  sealed trait Call {

    /** Returns the [[Action.Key]] of the call, which should be presented in any implementation of [[Call]]. */
    def actionKey: Action.Key
  }

  /** Contains implementations of [[Call]]. */
  object Call {

    /** Represents the case when mode called using it's name, version and action name. */
    final case class Full(
      modeKey: Mode.Key,
      override val actionKey: Action.Key
    ) extends Call

    /** Represents the case when mode called using it's name and action name.
      * In this case the version which is marked as latest will be used.
      */
    final case class Latest(
      modeName: Mode.Name,
      override val actionKey: Action.Key
    ) extends Call

    /** Creates new mode full call.
      *
      * @note It's just an alias for mode full call main constructor, added just for simplifying the creation of call.
      */
    def apply(modeKey: Mode.Key, actionKey: Action.Key): Mode.Call.Full = Full(modeKey, actionKey)

    /** Creates new mode latest call.
      *
      * @note It's just an alias for mode latest call main constructor, added just for simplifying the creation of call.
      */
    def apply(modeName: Mode.Name, actionKey: Action.Key): Mode.Call.Latest = Latest(modeName, actionKey)
  }

  type CallOverrides = Map[NonEmptyList[Call], NonEmptyList[Call]]

  // region Mode implementations

  private class Basic[F[_] : Sync : RunContext, A, B](
    f: MFunction[F, A, B],
    override val definition: Definition.Basic
  )(
    implicit
    modeRequestDecoder: ModeRequestDecoder[A],
    modeResponseEncoder: ModeResponseEncoder[B]
  ) extends Mode[F] {

    private[this] def validateRequest(request: Request): Option[ModeError] =
      checkRequestTypeSupport(definition, request) <+>
      Option.when(
        request.calls
          .map(definition.suitsFor)
          .filterNot(x => x)
          .nonEmpty
      )(InvalidRequest.InappropriateCall.ByModeNameOrVersion(definition, request): ModeError)

    override def apply(request: Request): EitherT[F, ModeError, Response] =
      validateRequest(request) match {
        case Some(error) => EitherT.leftT[F, Response](error)
        case None =>
          if (request.calls.size > 1) EitherT.fromEither[F](this andThen this).flatMap(_.apply(request))
          else
            for {
              modeRequest  <- EitherT.fromEither[F](modeRequestDecoder.apply(request.asSingle))
              modeResponse <- f(modeRequest)
              abstractResponse <- EitherT.fromEither[F](
                modeResponseEncoder.apply(modeResponse)(request)
              )
            } yield abstractResponse
      }
  }

  private class OrElse[F[_] : Sync : RunContext](left: Mode[F], right: Mode[F]) extends Mode[F] {

    override val definition: Definition.OrElse =
      Definition.OrElse(left.definition, right.definition)

    private[this] def validateRequestAndGetMode(request: Request): Either[ModeError, Mode[F]] =
      checkRequestTypeSupport(this.definition, request) match {
        case Some(error: ModeError) => Left(error)
        case None =>
          if (request.calls.size == 1) {
            if (definition.left.suitsFor(request.calls.head)) Right(left)
            else if (definition.right.suitsFor(request.calls.head)) Right(right)
            else Left(InvalidRequest.InappropriateCall.ByModeNameOrVersion(definition, request))
          } else if (request.calls
                       .map(definition.suitsFor)
                       .filterNot(x => x) // TODO: one of my best lines
                       .isEmpty) {
            this andThen this
          } else {
            Left(InvalidRequest.InappropriateCall.ByModeNameOrVersion(definition, request))
          }
      }

    override def apply(request: Request): EitherT[F, ModeError, Response] =
      validateRequestAndGetMode(request) match {
        case Left(error) => EitherT.leftT[F, Response](error)
        case Right(mode) => mode(request)
      }
  }

  private class AndThen[F[_] : Sync : RunContext](
    first: Mode[F],
    next: Mode[F]
  )(
    implicit
    resAndReqCombiner: (Mode.Response, Mode.Request) => Either[ModeError, String],
    resCombiner: (Mode.Response, Mode.Response) => Either[ModeError, String]
  ) extends Mode[F] {

    override def definition: Definition = Definition.AndThen(first.definition, next.definition)

    private[this] def validateAndPrepareRequest(
      request: Request
    ): Either[ModeError, ((Mode[F], Request), (Mode[F], Request))] = {
      def ifFirstNotAndThen(
        f: Mode[F],
        n: Mode[F]
      ): Either[ModeError, ((Mode[F], Request), (Mode[F], Request))] = {
        // FIXME: unsafe operation .get
        val firstRequest = request.dropTail(OutputType.Json)
        val firstCall = firstRequest.calls.head
        val maybeNextRequest = request.dropFirst()
        val maybeNextCall = maybeNextRequest.map(_.calls.head)
        val error =
          checkRequestTypeSupport(f.definition, firstRequest) <+>
          Option.when(!f.definition.suitsFor(firstCall))(
            InvalidRequest.InappropriateCall.ByModeNameOrVersion(definition, request): ModeError
          ) <+>
          (maybeNextCall match {
            case Some(call) =>
              Option.when(!n.definition.suitsFor(call))(
                InvalidRequest.InappropriateCall.ByModeNameOrVersion(definition, request): ModeError
              )
            case None => Some(InvalidRequest.InappropriateCall.ByNumberOfCalls(definition, request))
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

    override def apply(request: Request): EitherT[F, ModeError, Response] =
      validateAndPrepareRequest(request) match {
        case Left(error) => EitherT.leftT[F, Response](error)
        case Right(((m1, r1), (m2, r2))) =>
          for {
            firstRes  <- m1(r1)
            newReq    <- EitherT.fromEither[F](resAndReqCombiner(firstRes, request))
            secondRes <- m2(r2.copy(body = newReq))
            finalBody <- EitherT.fromEither[F](resCombiner(firstRes, secondRes))
          } yield secondRes.copy(body = finalBody)
      }
  }

  sealed abstract class Dynamic[F[_] : Sync : RunContext] extends Mode[F]

  object Dynamic {

    class Web[F[_] : Sync : RunContext, A, B](config: Web.Config) extends Dynamic[F] {
      override def definition: Definition = ???
      override def apply(request: Mode.Request): EitherT[F, ModeError, Mode.Response] = ???
    }

    object Web {
      case class Config(modeName: String, host: String)
    }
  }

  private class Alias[F[_] : Sync : RunContext](
    mode: Mode[F]
  )(callOverrides: CallOverrides, definitionCreator: Definition => Definition.Alias)
      extends Mode[F] {
    override val definition: Definition = definitionCreator(mode.definition)

    override def apply(request: Request): EitherT[F, ModeError, Response] = ???
  }

  private def checkRequestTypeSupport(
    definition: Definition,
    request: Mode.Request
  ): Option[ModeError] =
    Option.when(!definition.doesSupport(request.inputType))(
      InvalidRequest.UnsupportedInputType(definition, request): ModeError
    ) <+>
    Option.when(!definition.doesSupport(request.outputType))(
      InvalidRequest.UnsupportedOutputType(definition, request): ModeError
    )

  // endregion

  // region Errors

  sealed trait InvalidRequest extends ModeError
  object InvalidRequest {
    case class UnsupportedInputType(modeDef: Definition, request: Request) extends InvalidRequest
    case class UnsupportedOutputType(modeDef: Definition, request: Request) extends InvalidRequest
    sealed trait InappropriateCall extends InvalidRequest
    object InappropriateCall {
      case class ByModeNameOrVersion(modeDef: Definition, request: Request) extends InappropriateCall
      case class ByNumberOfCalls(modeDef: Definition, request: Request) extends InappropriateCall
    }
  }

  case class ModesNotCombinable(first: Definition, second: Definition) extends ModeError

  sealed trait ModeInitializationError extends GrafexError
  object ModeInitializationError {
    final case class NeededCallUnsupported() extends ModeInitializationError
  }

  // endregion

  // region Definitions

  final class Dependency[F[_] : Sync : RunContext] private (
    val mode: Mode[F],
    private val definition: Definition.Callable
  ) {
    def key: Mode.Key = definition.modeKey
    def generateCall: Mode.Call = ???
  }

//  case class DependencyCreationError(mode: Mode[_], definition: Definition.Callable) extends GrafexError

  final case class Key(name: Name, version: Version)

  sealed trait Definition {
    def suitsFor(call: Mode.Call): Boolean
    def doesSupport(inputType: InputType): Boolean
    def doesSupport(outputType: OutputType): Boolean
  }

  object Definition {

    sealed trait Callable extends Definition {
      def modeKey: Mode.Key
      def isLatest: Boolean = false
    }

    case class Basic(
      override val modeKey: Mode.Key,
      description: Option[String] = None,
      supportedInputTypes: Set[InputType],
      supportedOutputTypes: Set[OutputType],
      actions: Set[Action.Definition],
      override val isLatest: Boolean = false
    ) extends Definition
        with Callable {
      private[this] def suitsOneAction(actionKey: Action.Key): Boolean = // TODO: implement
        true //actions.map(_.suitsFor(actionKey)).size == 1

      override def suitsFor(call: Mode.Call): Boolean = call match {
        case Call.Full(modeKey, actionKey)    => this.modeKey == modeKey && suitsOneAction(actionKey)
        case Call.Latest(modeName, actionKey) => isLatest && this.modeKey.name == modeName && suitsOneAction(actionKey)
      }

      override def doesSupport(inputType: InputType): Boolean = supportedInputTypes.contains(inputType)
      override def doesSupport(outputType: OutputType): Boolean = supportedOutputTypes.contains(outputType)

      def toLatest: Basic = this.copy(isLatest = true)
    }

    case class OrElse(
      left: Definition,
      right: Definition
    ) extends Definition {
      private[this] def or[A](f: (Definition, A) => Boolean)(a: A): Boolean = f(left, a) || f(right, a)

      override def suitsFor(call: Mode.Call): Boolean = or[Mode.Call](_.suitsFor(_))(call)
      override def doesSupport(inputType: InputType): Boolean = or[InputType](_.doesSupport(_))(inputType)
      override def doesSupport(outputType: OutputType): Boolean = or[OutputType](_.doesSupport(_))(outputType)
    }

    case class AndThen(
      first: Definition,
      next: Definition
    ) extends Definition {
      // FIXME: impossible to implement
      override def suitsFor(call: Mode.Call): Boolean = ???
      override def doesSupport(inputType: InputType): Boolean = first.doesSupport(inputType)
      override def doesSupport(outputType: OutputType): Boolean = next.doesSupport(outputType)
    }

    case class Alias(
      overriddenModeKey: Mode.Key,
      description: Option[String] = None,
      override val isLatest: Boolean = false
    )(
      definition: Definition
    ) extends Definition
        with Callable {

      override val modeKey: Mode.Key = overriddenModeKey

      override def suitsFor(call: Mode.Call): Boolean = call match {
        case Call.Full(modeKey, _)    => overriddenModeKey == modeKey
        case Call.Latest(modeName, _) => isLatest && overriddenModeKey.name == modeName
      }
      override def doesSupport(inputType: InputType): Boolean = definition.doesSupport(inputType)
      override def doesSupport(outputType: OutputType): Boolean = definition.doesSupport(outputType)

      def toLatest: Alias = this.copy(isLatest = true)(definition)
    }
  }

  case class Name(name: String) {
    override def toString: String = name
  }

  object Name {
    def orElse(left: Name, right: Name): Name = Name(s"${left.name}|${right.name}")
    def andThen(first: Name, next: Name): Name = Name(s"${first.name}>${next.name}")
  }

  final case class Version(id: String) {
    override def toString: String = id
  }

  object Version {

    implicit val versionOrdering: Ordering[Version] = (x: Version, y: Version) => {
      val xs: List[String] = x.id.split(".", 3).toList
      val ys: List[String] = y.id.split(".", 3).toList
      xs.zipAll(ys, "0", "0")
        .map(p => p._1.compareTo(p._2))
        .find(_ != 0)
        .getOrElse(0)
    }
  }

  // TODO: move out from Mode object
  object Action {

    final case class Key(name: Name)

    case class Name(name: String) {
      override def toString: String = name
    }

    case class Definition(
      actionKey: Action.Key,
      description: Option[String] = None,
      params: Set[Param.Definition]
    ) {
      def suitsFor(actionKey: Action.Key): Boolean = this.actionKey == actionKey
    }
  }

  // TODO: move out from Mode object
  object Param {
    case class Name(name: String) {
      override def toString: String = name
    }

    object Name {
      def fromString(name: String) = new Name(name)
    }

    case class Definition(name: Name, description: Option[String] = None)

    // TODO: move under Definition object
    def apply(name: String, description: Option[String] = None): Definition =
      Definition(Name.fromString(name), description)
  }

  // endregion
}
