package com.grafex.core
package graph

trait NodeResultDecoder[A, B] {
  def decode(a: A): Either[GraphDataSource.Error, Node[B]]
}

/** Factory for [[NodeResultDecoder]]. */
object NodeResultDecoder {
  //noinspection ConvertExpressionToSAM
  def instance[A, B](f: A => Either[GraphDataSource.Error, Node[B]]): NodeResultDecoder[A, B] =
    new NodeResultDecoder[A, B] {
      override def decode(a: A): Either[GraphDataSource.Error, Node[B]] = f(a)
    }
}
