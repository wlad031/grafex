package com.grafex.core.listeners

import cats.data.EitherT
import cats.effect.{ ExitCode, IO }
import com.grafex.core.{ GrafexError, Mode }
import com.grafex.core.boot.Startup

object SocketListener {

  def apply(startupCtx: Startup.Context, modeContainer: Mode[IO]): EitherT[IO, GrafexError, ExitCode] = {
    ???
  }
}
