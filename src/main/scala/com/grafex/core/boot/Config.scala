package com.grafex.core.boot

import cats.Semigroup
import cats.data.EitherT
import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.grafex.core.GrafexError
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Config {

  // TODO: implement reading of multiple configs in the correct way
  def read(context: Startup.Context): EitherT[IO, ConfigReadingError, GrafexConfig] = {
    EitherT(IO {
      val defaultConfig: Result[GrafexConfig] = ConfigSource.default.load[GrafexConfig]
      val providedConfigs: List[Result[GrafexConfig]] =
        context.configPaths.map(ConfigSource.file).map(_.load[GrafexConfig])
      if (providedConfigs.nonEmpty) {
        ???
      } else {
        defaultConfig.leftMap(f => ConfigReadingError(f.toString()))
      }
    })
  }

  case class GrafexConfig(
    metaDataSource: GrafexConfig.MetaDataSourceConfig
  )

  object GrafexConfig {

    sealed trait MetaDataSourceConfig
      case class Foo(
        url: String
      ) extends MetaDataSourceConfig

    implicit val semigroup: Semigroup[GrafexConfig] = (x: GrafexConfig, y: GrafexConfig) => {
      y // TODO: implement
    }
  }

  case class ConfigReadingError(msg: String) extends GrafexError
}
