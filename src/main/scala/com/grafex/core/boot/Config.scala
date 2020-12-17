package com.grafex
package core
package boot

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.toBifunctorOps
import com.typesafe.config.{ ConfigFactory, ConfigParseOptions, Config => TypesafeConfig }
import io.circe.Json
import pureconfig.ConfigReader.Result
import pureconfig.backend.ErrorUtil.unsafeToReaderResult
import pureconfig.error.CannotReadFile
import pureconfig.generic.auto._
import pureconfig.module.circe._
import pureconfig.{ ConfigObjectSource, ConfigSource }

import java.nio.file.Path

/** Contains all the functionality for reading application configuration files.
  *
  * @example {{{
  * val startupContext: Startup.Context = ??? // some startup context
  *
  * import com.grafex.core.boot.Config
  * import com.grafex.core.boot.Config.{ ConfigurationReadingError, GrafexConfiguration }
  *
  * val config: EitherT[IO, ConfigurationReadingError, GrafexConfiguration] = Config.read(startupContext)()
  * }}}
  */
object Config {

  /** Loads the [[GrafexConfiguration]].
    *
    * Sources (descending precedence):
    *   1. config files specified as argument parameters;
    *   2. default config;
    *   3. "baked" in the application resources.
    *
    * @param context the startup context
    * @param defaultConfigPath default config path (default value
    *                          is built using [[Config.buildHomeConfigPath(Startup.Context)]])
    * @return [[EitherT]] of [[IO]] with loaded config or [[ConfigurationReadingError]]
    */
  def load(
    context: Startup.Context
  )(
    defaultConfigPath: Path = buildHomeConfigPath(context)
  ): EitherT[IO, ConfigurationReadingError, GrafexConfiguration] = {

    val sources = context.configPaths.reverse
      .map(ConfigSource.file)
      .foldLeft(ConfigSource.empty)(_ withFallback _)
      .withFallback(optionalFileSource(defaultConfigPath))
      .withFallback(ConfigSource.default)

    val load = IO {
      sources.load[GrafexConfiguration].leftMap(f => ConfigurationReadingError(f.prettyPrint(2)))
    }

    EitherT(load)
  }

  /** Returns the default config path which will be located in `$HOME/.config/grafex/grafex.conf`.
    *
    * @param context the startup context
    */
  def buildHomeConfigPath(context: Startup.Context): Path = {
    val dirPath = context.userHome.resolve(".config/grafex")
    dirPath.resolve("grafex.conf")
  }

  /** Contains all the application configurations. */
  case class GrafexConfiguration(
    accountId: String,
    metaDataSource: GrafexConfiguration.MetaDataSourceConfig,
    modes: List[Json]
  )

  /** Contains inner structures of the [[GrafexConfiguration]] */
  object GrafexConfiguration {

    sealed trait MetaDataSourceConfig

    case class Foo(
      url: String
    ) extends MetaDataSourceConfig
  }

  /** Represents an error of config reading. */
  case class ConfigurationReadingError(msg: String) extends GrafexError

  /** `pureconfig` settings which allow to not fail on non-existing configuration files. */
  private val notStrictSettings: ConfigParseOptions = ConfigParseOptions.defaults.setAllowMissing(true)

  /** Parses the config file which can be missing. */
  private def parseOptionalFile(path: Path): Result[TypesafeConfig] = {
    unsafeToReaderResult(
      ConfigFactory.parseFile(path.toFile, notStrictSettings),
      onIOFailure = Some(CannotReadFile(path, _))
    )
  }

  /** Returns [[ConfigObjectSource]] for file which can be missing. */
  private def optionalFileSource(path: Path): ConfigObjectSource = ConfigObjectSource(parseOptionalFile(path))
}
