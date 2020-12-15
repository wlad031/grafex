package com.grafex.core.boot

import cats.Semigroup
import cats.data.EitherT
import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.grafex.core.GrafexError
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.nio.file.Files

object Config {

  private val DEFAULT_CONFIG_DIR = ".config/grafex"
  private val DEFAULT_CONFIG_FILE_NAME = "grafex.conf"

  def read(context: Startup.Context): EitherT[IO, ConfigReadingError, GrafexConfig] = {

    val dirPath = context.userHome.resolve(DEFAULT_CONFIG_DIR)
    val filePath = dirPath.resolve(DEFAULT_CONFIG_FILE_NAME)

    val sources = context.configPaths
      .reverse
      .map(ConfigSource.file)
      .foldLeft(ConfigSource.empty)(_ withFallback _)
      .withFallback(ConfigSource.file(filePath))
      .withFallback(ConfigSource.default)

    val load = IO {
      if (!Files.exists(filePath)) {
        if (!Files.exists(dirPath)) Files.createDirectories(dirPath)
        Files.createFile(filePath)
      }
      sources.load[GrafexConfig].leftMap(f => ConfigReadingError(f.toString()))
    }

    EitherT(load)
  }

  case class GrafexConfig(
    accountId: String,
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
