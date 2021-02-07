package com.grafex.core
package conversion

import com.grafex.core.definitions.action

trait ActionRequestDecoder[+AIn] extends PartialFunction[ModeRequest, EitherE[AIn]] { self =>

  protected def applicableFor: definitions.action.Id

  def applyOrElse[AIn1 >: AIn](request: ModeRequest, default: ModeRequest => EitherE[AIn1]): EitherE[AIn1] =
    if (isDefinedAt(request)) apply(request) else default(request)

  def orElse[AIn1 >: AIn](that: ActionRequestDecoder[AIn1]): ActionRequestDecoder[AIn1] =
    new ActionRequestDecoder.OrElse[AIn1, AIn, AIn1](self, that)

  def asDefault: ActionRequestDecoder[AIn] = new ActionRequestDecoder[AIn] {
    override def applicableFor: action.Id = self.applicableFor
    override def isDefinedAt(request: ModeRequest): Boolean = request.calls.head.actionId == self.applicableFor
    override def apply(request: ModeRequest): EitherE[AIn] = self.apply(request)
  }
}

object ActionRequestDecoder {

  def instance[AIn](
    f: ModeRequest => EitherE[AIn]
  )(
    implicit
    actionId: definitions.action.Id
  ): ActionRequestDecoder[AIn] = new ActionRequestDecoder[AIn] {
    override protected def applicableFor: action.Id = actionId
    override def isDefinedAt(request: ModeRequest): Boolean = request.calls.head.actionId == applicableFor
    override def apply(request: ModeRequest): EitherE[AIn] = f(request)
  }

  final class OrElse[+A, +A1 <: A, +A2 <: A](left: ActionRequestDecoder[A1], right: ActionRequestDecoder[A2])
      extends ActionRequestDecoder[A] {
    override protected def applicableFor: action.Id = left.applicableFor
    override def isDefinedAt(request: ModeRequest): Boolean = left.isDefinedAt(request) || right.isDefinedAt(request)
    override def apply(request: ModeRequest): EitherE[A] =
      if (left.isDefinedAt(request)) left.apply(request) else right.apply(request)
  }
}
