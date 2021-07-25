package dev.vgerasimov.grafex
package core
package definitions

import scala.annotation.StaticAnnotation

/** Contains all available annotations for describing mode/action/property definitions. */
object annotations {

  /** Allows to define mode's identifier. */
  final case class modeId(name: String, version: String) extends StaticAnnotation

  /** Allows to define action's identifier. */
  final case class actionId(name: String) extends StaticAnnotation

  /** Allows to define a description of a mode/action/property. */
  final case class description(s: String) extends StaticAnnotation
}
