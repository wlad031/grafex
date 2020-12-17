package com.grafex.core.listeners

import cats.data.EitherT
import cats.effect.{ ExitCode, IO }
import com.grafex.core.boot.Startup
import com.grafex.core.{ GrafexError, Mode }

object SocketListener {

  def apply(startupCtx: Startup.Context, modeContainer: Mode[IO]): EitherT[IO, GrafexError, ExitCode] = {
    ???
  }
}
