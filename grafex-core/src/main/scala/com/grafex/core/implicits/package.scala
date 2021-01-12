package com.grafex.core

import cats.Semigroup
import cats.effect.ExitCode

package object implicits {

  implicit val exitCodeSemigroup: Semigroup[ExitCode] = (x: ExitCode, y: ExitCode) =>
    if (x == ExitCode.Success && y == ExitCode.Success) ExitCode.Success else ExitCode.Error
}
