package com.grafex.core

import cats.data.NonEmptyList
import cats.data.Validated.Valid
import cats.syntax.either._
import cats.syntax.validated._
import com.grafex.core.errors.GrafexError

/** Used for parsing strings representing request options into maps with string keys and string values.
  *
  * Pattern of request options strings:
  *
  *   {{{ "key=value[;key=value...]" }}}
  *
  * In general, it's a string, where key-value pairs separated by `;` (semicolon) character
  * and key separated from it's value by `=` (equals) character.
  *
  * @note Separators can be overridden by passing additional parameters.
  *
  * @example {{{
  * import com.grafex.core.ModeRequestOptionsParser.parse
  * import com.grafex.core.ModeRequestOptionsParser.ParseError
  *
  * val params: Right[Map[String, String]] = parse("key1=value1;key2=value2")
  * val error: Left[ParseError] = parse("key1=;key2=value2")
  * }}}
  *
  * @note Empty strings are valid and will be parsed into empty maps.
  */
object ModeRequestOptionsParser {

  /** Represent generic mode request options parsing error. */
  sealed trait ParseError extends GrafexError

  /** Contains implementations of [[ParseError]]. */
  object ParseError {
    final case class MultipleErrors(errors: NonEmptyList[ParseError]) extends ParseError
    final case class MissingValue(key: String) extends ParseError
  }

  /** Parses provided string into map of key-value pairs found in this string.
    *
    * @param s string to be parsed
    * @param kvSep string used for separating keys from their values
    * @param pairsSep string used for separating key-values pairs from each other
    * @return either map of options or [[ParseError]] in case if parsing not successful
    */
  def parse(s: String, kvSep: String = "=", pairsSep: String = ";"): Either[ParseError, Map[String, String]] = s match {
    case "" => Map[String, String]().asRight
    case ne =>
      ne.split(pairsSep)
        .filterNot(s => s == "")
        .map(p => {
          val spl = p.split(kvSep, 2)
          if (spl.length == 1) ParseError.MissingValue(spl(0)).invalidNel
          else Valid(NonEmptyList((spl(0), spl(1)), Nil))
        })
        .reduce(_ combine _)
        .toEither match {
        case Left(errors) =>
          errors match {
            case NonEmptyList(error, Nil) => error.asLeft
            case nel                      => ParseError.MultipleErrors(nel).asLeft
          }
        case Right(nel) => nel.toList.toMap.asRight
      }
  }
}
