package dev.vgerasimov.grafex
package core

final case class Node[M](id: String, labels: List[String], source: Node.Source, metadata: M)

object Node {
  object labels {
    val Account: String = "Account"
    val DataSource: String = "DataSource"
  }

  sealed trait Source
  object Source {
    final case class Itself() extends Source
  }
}
