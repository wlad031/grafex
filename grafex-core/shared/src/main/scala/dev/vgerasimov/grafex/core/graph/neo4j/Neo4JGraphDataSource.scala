package dev.vgerasimov.grafex
package core
package graph
package neo4j

import cats.Applicative
import cats.data.EitherT
import cats.instances.either._
import cats.syntax.bifunctor._
import neotypes.exceptions.ConversionException
import neotypes.implicits.mappers.parameters.StringParameterMapper
import neotypes.implicits.mappers.values.{StringValueMapper, iterableValueMapper}
import neotypes.implicits.syntax.cypher._
import neotypes.mappers.{ResultMapper, TypeHint, ValueMapper}
import org.neo4j.driver.Value

import scala.util.{Failure, Success, Try}

/** The implementation of [[GraphDataSource]] which uses Neo4J as a database backend.
  *
  * @param neo4jDriver neotypes' representation of Neo4J database driver
  * @param async$F$0 neotypes' representation of Async type class for the effect F
  * @param F Applicative type class for the effect F
  * @tparam F the effect type
  */
class Neo4JGraphDataSource[F[_] : neotypes.Async](neo4jDriver: neotypes.Driver[F])(implicit F: Applicative[F])
    extends GraphDataSource[F] {
  import Neo4JGraphDataSource.{ genericNodeResultDecoder, genericNodeResultMapper, ValuePairList }

  override protected type Dec = ValuePairList

  override def createNode(): EitherT[F, _, _] = ???

  override def updateNode(): EitherT[F, _, _] = ???

  override def deleteNode(): EitherT[F, _, _] = ???

  override def getNode(
    id: String
  ): EitherT[F, GraphDataSource.Error, Node[Map[String, String]]] = EitherT {
    neo4jDriver.readSession(s => {
      Try(
        c""" 
         MATCH (n {id: $id})
         RETURN
           n.id,
           labels(n),
           CASE
             WHEN n.source IS NULL THEN "itself"
             ELSE n.source
           END AS source,
           CASE n.source
             WHEN "itself" THEN n
             ELSE n
           END
         """
          .query[Node[Map[String, String]]]
          .single(s)
      ) match {
        case Failure(e) => F.pure(Left(GraphDataSource.Error.SingleError(e)))
        case Success(v) => F.map(v)(Right(_))
      }
    })
  }

  override def createRelationship(): EitherT[F, _, _] = ???

  override def updateRelationship(): EitherT[F, _, _] = ???

  override def deleteRelationship(): EitherT[F, _, _] = ???
}

/** Contains implicits for [[Neo4JGraphDataSource]]. */
object Neo4JGraphDataSource {

  private type ValuePairList = (List[(String, Value)], Option[TypeHint])

  /** neotypes' [[ValueMapper]] for "id" field. */
  private val idMapper: ValueMapper[String] = StringValueMapper

  /** neotypes' [[ValueMapper]] for "labels" field. */
  private val labelsMapper: ValueMapper[List[String]] = iterableValueMapper[String, List]

  /** neotypes' [[ResultMapper]] for node "source" field */
  private val sourceMapper: ValueMapper[Node.Source] = ValueMapper.instance { (name: String, value: Option[Value]) =>
    {
      value match {
        case Some(s) =>
          s.asString() match {
            case "itself" => Right(Node.Source.Itself())
            case x        => Left(ConversionException(s"Unknown source type: $x"))
          }
        case None => Right(Node.Source.Itself())
      }
    }
  }

  private implicit def genericNodeResultDecoder: NodeResultDecoder[ValuePairList, Map[String, String]] = {
    import neotypes.implicits.all._
    val metadataMapper: ResultMapper[Map[String, String]] = mapResultMapper

    NodeResultDecoder.instance { valuePairList: ValuePairList =>
      {
        val (kv, th) = valuePairList
        for {
          idKV <- kv.headOption
            .toRight(GraphDataSource.Error.IdNotFound())
          labelsKV <- kv
            .drop(1)
            .headOption
            .toRight(GraphDataSource.Error.LabelsNotFound())
          sourceKV <- kv
            .drop(2)
            .headOption
            .toRight(GraphDataSource.Error.SourceNotFound())
          id <- idMapper
            .to(idKV._1, Some(idKV._2))
            .leftMap(_ => GraphDataSource.Error.CannotDecodeId())
          labels <- labelsMapper
            .to(labelsKV._1, Some(labelsKV._2))
            .leftMap(_ => GraphDataSource.Error.CannotDecodeLabels())
          source <- sourceMapper
            .to(sourceKV._1, Some(sourceKV._2))
            .leftMap(t => GraphDataSource.Error.CannotDecodeSource(t))
          metadata <- metadataMapper
            .to(kv.drop(3), th)
            .leftMap(_ => GraphDataSource.Error.CannotDecodeMetadata())
        } yield Node(id, labels, source, metadata)
      }
    }
  }

  private implicit def genericNodeResultMapper[B](
    implicit
    nrd: NodeResultDecoder[ValuePairList, B]
  ): ResultMapper[Node[B]] = ResultMapper.instance { (ls: List[(String, Value)], th: Option[TypeHint]) =>
    {
      nrd.decode((ls, th)).leftMap(e => ConversionException(e.toString))
    }
  }
}
