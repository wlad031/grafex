package com.grafex.core

import cats.data.EitherT

trait MetaDataSource[F[_]] {
  def getDataSourceById(id: String): EitherT[F, MetaDataSource.Error, DataSourceMetadata]
}

object MetaDataSource {
  sealed trait Error
  object Error {
    case class DataSourceNotFound(id: String) extends Error
    case class InvalidDataSourceType(id: String, typ: String) extends Error
  }
}
