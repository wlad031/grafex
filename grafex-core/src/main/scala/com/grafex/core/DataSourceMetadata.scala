package com.grafex.core

import com.grafex.core.DataSourceMetadata.Id

sealed trait DataSourceMetadata {
  val id: Id
}

object DataSourceMetadata {
  final case class Id(s: String)

  final case class Virtual(override val id: Id) extends DataSourceMetadata
}
