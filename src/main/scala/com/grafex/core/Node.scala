package com.grafex.core

sealed trait DataSourceConnection {
  val dataSourceMetadata: DataSourceMetadata
}

object DataSourceConnection {

  case class Mongo(
    override val dataSourceMetadata: DataSourceMetadata.Mongo,
    id: String
  ) extends DataSourceConnection

  case class Mysql(
    override val dataSourceMetadata: DataSourceMetadata.Mysql,
    id: String
  ) extends DataSourceConnection

  case class FileSystem(
    override val dataSourceMetadata: DataSourceMetadata.FileSystem,
    path: String
  ) extends DataSourceConnection

  case class Neo4J(
    override val dataSourceMetadata: DataSourceMetadata.Virtual,
    id: String
  ) extends DataSourceConnection
}

case class Node(dataSourceConnection: DataSourceConnection)

object Node {
  object labels {
    val AccountLabel: String = "Account"
  }
}

case class Link(from: Node, to: Node)
