package com.grafex.core

import cats.data.EitherT
import cats.effect.{ContextShift, IO, Resource}
import com.grafex.core.boot.Config.GrafexConfig
import com.grafex.core.internal.neo4j.{logging => Neo4JLogging}
import neotypes.cats.effect.implicits._
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.cypher._
import neotypes.{GraphDatabase, Session}
import org.neo4j.driver.{AuthTokens, Config}

class Neo4jMetaDataSource(config: GrafexConfig.Foo) extends MetaDataSource[IO] {

  // TODO: pass context shift as a parameter
  private[this] implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  override def getDataSourceById(id: String): EitherT[IO, MetaDataSource.Error, DataSourceMetadata] = {
    val session: Resource[IO, Session[IO]] = for {
      driver <- GraphDatabase.driver[IO](
        config.url,
        AuthTokens.none(),
        Config.builder().withLogging(Neo4JLogging()).build()
      )
      session <- driver.session
    } yield session

    EitherT(session.use { s =>
      c"MATCH (p:DS {id: $id}) RETURN p.id, p.typ"
        .query[Option[(String, String)]]
        .single(s)
        .map({
          case Some((id: String, typ: String)) =>
            typ match {
              case "mongo"   => Right(DataSourceMetadata.Mongo(DataSourceMetadata.Id(id)))
              case "mysql"   => Right(DataSourceMetadata.Mysql(DataSourceMetadata.Id(id)))
              case "filesys" => Right(DataSourceMetadata.FileSystem(DataSourceMetadata.Id(id)))
              case s         => Left(MetaDataSource.Error.InvalidDataSourceType(id, s))
            }
          case None => Left(MetaDataSource.Error.DataSourceNotFound(id))
        })
    })
  }

  override def createNode(dataSourceId: Option[String]): EitherT[IO, MetaDataSource.Error, Node] = {
    val session: Resource[IO, Session[IO]] = for {
      driver <- GraphDatabase.driver[IO](
        config.url,
        AuthTokens.none(),
        Config.builder().withLogging(Neo4JLogging()).build()
      )
      session <- driver.session
    } yield session

//    EitherT(session.use { s =>
//      c"CREATE"
//    })
    ???
  }
}
