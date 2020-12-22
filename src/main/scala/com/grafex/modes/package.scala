package com.grafex

import com.grafex.core.{ Mode, ModeCallsParser }

package object modes {

  /** Runs parsing of single mode call and unsafely returns the result.
    *
    * @throws RuntimeException in case if parsing error happened
    *
    * @todo Make it package private.
    *
    * @note Should not be used in dynamic context as it's not safe.
    */
  def unsafeParseSingleModeCall(s: String): Mode.Call = ModeCallsParser.parse(s) match {
    case Left(error)  => sys.error(s"Unexpected error $error")
    case Right(value) => value.head
  }
}
