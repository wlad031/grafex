package com.grafex.core.boot

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.grafex.core.GrafexError
import com.typesafe.config.{ ConfigFactory, ConfigParseOptions, Config => TConfig }
import pureconfig.ConfigReader.Result
import pureconfig.backend.ErrorUtil.unsafeToReaderResult
import pureconfig.error.CannotReadFile
import pureconfig.generic.auto._
import pureconfig.{ ConfigObjectSource, ConfigSource }

import java.nio.file.Path

object Config {

  def read(
    context: Startup.Context
  )(
    defaultConfig: Path = homeConfig(context)
  ): EitherT[IO, ConfigReadingError, GrafexConfig] = {

    val sources = context.configPaths.reverse
      .map(ConfigSource.file)
      .foldLeft(ConfigSource.empty)(_ withFallback _)
      .withFallback(optionalFileSource(defaultConfig))
      .withFallback(ConfigSource.default)

    val load = IO {
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
  }

  case class ConfigReadingError(msg: String) extends GrafexError

  private val notStrictSettings: ConfigParseOptions = ConfigParseOptions.defaults.setAllowMissing(true)

  private def parseOptionalFile(path: Path): Result[TConfig] = unsafeToReaderResult(
    ConfigFactory.parseFile(path.toFile, notStrictSettings),
    onIOFailure = Some(CannotReadFile(path, _))
  )

  private def optionalFileSource(path: Path): ConfigObjectSource = ConfigObjectSource(parseOptionalFile(path))

  def homeConfig(context: Startup.Context): Path = {
    val dirPath = context.userHome.resolve(".config/grafex")
    dirPath.resolve("grafex.conf")
  }
}
