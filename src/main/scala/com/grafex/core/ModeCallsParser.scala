package com.grafex.core

import cats.data.NonEmptyList
import cats.syntax.either._
import com.grafex.core.definitions._
import com.grafex.core.modeFoo.Mode

/** Used for parsing strings into [[Mode.Call]] objects.
  *
  * Single mode call pattern:
  *
  *   {{{ modeName[.modeVersion]/actionName }}}
  *
  * Mode version is optional and followed after mode name and single `.` (dot) character; action name is a substring
  * after mode name (and optionally - mode version) and `/` (slash) character.
  *
  * Multi mode call pattern is just several single mode calls separated with `>` (greater-than-sign) character.
  *
  * In general, the pattern of allowed strings for parsing:
  *
  *   {{{ mode1Name[.mode1Version]/action1Name[>mode2Name[.mode2Version]/action2Name]... }}}
  *
  * Allowed strings for parsing:
  *   1. `mode/action` - represents mode latest call [[Mode.Call.Latest]];
  *   1. `mode.1/action` - represents mode full call [[Mode.Call.Full]];
  *   1. `mode1/action1>mode2/action2` - represents multi mode call.
  *
  * @example {{{
  * import com.grafex.core.ModeCallsParser
  *
  * val latestCall: Right[NonEmptyList[Mode.Call]] = parse("modeName/actionName")
  * val versionedCall: Right[NonEmptyList[Mode.Call]] = parse("modeName.1.0.0/actionName")
  * val multiCall: Right[NonEmptyList[Mode.Call]] = parse("mode1Name.1.0.0/action1Name>mode2Name>action2Name")
  * }}}
  *
  * @see [[Mode.Call]]
  */
object ModeCallsParser {

  /** Represent generic mode calls parsing error. */
  sealed trait ParseError extends GrafexError

  /** Contains implementations of [[ParseError]]. */
  object ParseError {

    /** Parsing error which represents the case when action name is missing in the call. */
    final case class MissingAction(call: String) extends ParseError

    /** Parsing error which represents the case when provided for parsing string is empty. */
    final case class EmptyCall() extends ParseError

    /** Parsing error which represents the case when one of calls in the string is empty. */
    final case class EmptySubCall(call: String) extends ParseError

    /** Parsing error which represents some unknown case. */
    final case class InvalidCall(call: String) extends ParseError
  }

  /** Parses provided string into [[cats.data.NonEmptyList]] of [[Mode.Call]]s.
    *
    * @param s the string to be parsed
    * @return either [[cats.data.NonEmptyList]] of [[Mode.Call]]s or [[ParseError]] in case of parsing not successful.
    */
  def parse(s: String): Either[ParseError, NonEmptyList[Mode.Call]] = s match {
    case "" => Left(ParseError.EmptyCall())
    case calls =>
      calls
        .split(">")
        .toList
        .map({
          case "" => Left(ParseError.EmptySubCall(calls))
          case x =>
            val ss = x.split("/", 2)
            if (ss.length == 2) {
              val mode = ss(0)
              val action = ss(1)
              val ms = mode.split("""\.""", 2)

              if (ms.length == 1) Right(createLatestCall(ms(0), action))
              else Right(createFullCall(ms(0), ms(1), action))

            } else {
              Left(ParseError.MissingAction(x))
            }
        })
        .reduce(_ combine _)
  }

  /** Creates single full mode call. */
  private def createFullCall(modeName: String, version: String, actionName: String): NonEmptyList[Mode.Call] = {
    NonEmptyList(
      Mode.Call.Full(
        mode.Id(modeName, version),
        action.Id(actionName)
      ),
      Nil
    )
  }

  /** Creates single latest mode call. */
  private def createLatestCall(modeName: String, actionName: String): NonEmptyList[Mode.Call] = {
    NonEmptyList(
      Mode.Call.Latest(
        modeName,
        action.Id(actionName)
      ),
      Nil
    )
  }
}
