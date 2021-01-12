package com.grafex.core

import cats.Monad
import cats.effect.{ Clock, Sync }
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

trait ModeTestSuite {
  def createTestRunContext[F[_] : Sync](implicit M: Monad[F]): F[RunContext[F]] = {
    M.map(Slf4jLogger.fromName[F]("test"))(l =>
      new RunContext[F] {
        override val clock: Clock[F] = Clock.create[F]
        override val logger: Logger[F] = l
      }
    )
  }
}
