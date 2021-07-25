package dev.vgerasimov.grafex
package core
package internal

import org.neo4j.driver.{ Logger, Logging }
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success, Try }

package object neo4j {

  /** Contains some logging utilities for [[org.neo4j.driver]].
    *
    * [[org.neo4j.driver]] does have it's own loggers,
    * including SLF4J logging: [[org.neo4j.driver.internal.logging.Slf4jLogging]].
    * So, while building the config for Neo4J driver you can use it like so:
    * {{{
    *   Config.builder().withLogging(Slf4jLogging()).build()
    * }}}
    *
    * But loggers, created by this class won't have any prefix for their names, thus, it it's a bit hard
    * to configure them in the Logback configuration.
    *
    * That's why this implementation of the Neo4J logging exists: it is basically the same as
    * [[org.neo4j.driver.internal.logging.Slf4jLogging]], but uses `org.neo4j.driver.internal.overrides.` prefix
    * for all logger names.
    *
    * @example
    * {{{
    *   import dev.vgerasimov.grafex.core.internal.neo4j.{logging => Neo4JLogging}
    *   Config.builder().withLogging(Neo4JLogging()).build()
    * }}}
    *
    * @note If SL4J is not is the classpath, it returns provided (or default [[Logging.console]]) failover logging.
    *
    * @todo make it more private
    */
  object logging {

    /** Instantiates a new [[Slf4jLogger]] logger or returns the failover one. */
    def apply(failover: => Logging = Logging.console(java.util.logging.Level.ALL)): Logging =
      Try(Class.forName("org.slf4j.LoggerFactory")) match {
        case Failure(_) => failover
        case Success(_) =>
          (name: String) => new Slf4jLogger(LoggerFactory.getLogger(s"org.neo4j.driver.internal.overrides.$name"))
      }

    private class Slf4jLogger(val l: org.slf4j.Logger) extends Logger {
      private[this] def fmt(msg: String, params: AnyRef*): String = String.format(msg, params: _*)

      override def error(msg: String, cause: Throwable): Unit = if (l.isErrorEnabled) l.error(msg, cause)
      override def warn(msg: String, cause: Throwable): Unit = if (l.isWarnEnabled) l.warn(msg, cause)
      override def warn(msg: String, params: AnyRef*): Unit = if (l.isWarnEnabled) l.warn(fmt(msg, params: _*))
      override def info(msg: String, params: AnyRef*): Unit = if (l.isInfoEnabled) l.info(fmt(msg, params: _*))
      override def debug(msg: String, params: AnyRef*): Unit = if (isDebugEnabled) l.debug(fmt(msg, params: _*))
      override def trace(msg: String, params: AnyRef*): Unit = if (isTraceEnabled) l.trace(fmt(msg, params: _*))

      override def isTraceEnabled: Boolean = l.isTraceEnabled
      override def isDebugEnabled: Boolean = l.isDebugEnabled
    }
  }
}
