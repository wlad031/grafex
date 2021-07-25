package dev.vgerasimov.grafex
package core
package graph

import cats.data.EitherT

trait GraphDataSource[F[_]] {

  protected type Dec

  def createNode(): EitherT[F, _, _]
  def updateNode(): EitherT[F, _, _]
  def deleteNode(): EitherT[F, _, _]
  def getNode(id: String): EitherT[F, GraphDataSource.Error, Node[Map[String, String]]]

  def createRelationship(): EitherT[F, _, _]
  def updateRelationship(): EitherT[F, _, _]
  def deleteRelationship(): EitherT[F, _, _]
}

object GraphDataSource {

  /** Generic representation for [[GraphDataSource]]-related errors. */
  sealed trait Error

  /** Contains implementations of [[GraphDataSource.Error]] */
  object Error {
    final case class SingleError(ex: Throwable) extends Error
    final case class IdNotFound() extends Error
    final case class CannotDecodeId() extends Error
    final case class LabelsNotFound() extends Error
    final case class SourceNotFound() extends Error
    final case class CannotDecodeLabels() extends Error
    final case class CannotDecodeSource(t: Throwable) extends Error
    final case class CannotDecodeMetadata() extends Error
    final case class UnknownNodeSource(s: String) extends Error
  }
}
