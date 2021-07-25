package dev.vgerasimov.grafex
package core.listeners

import cats.data.EitherT
import cats.effect.{ ExitCode, IO }
import dev.vgerasimov.grafex.core.Mode
import dev.vgerasimov.grafex.core.boot.Startup
import dev.vgerasimov.grafex.core.errors.GrafexError

object SocketListener {

  def apply(startupCtx: Startup.Context, modeContainer: Mode[IO]): EitherT[IO, GrafexError, ExitCode] = {
    ???
  }
}
