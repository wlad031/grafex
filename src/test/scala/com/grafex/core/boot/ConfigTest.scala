package com.grafex.core.boot

import cats.effect.{ IO, Resource }
import org.scalatest.flatspec.AnyFlatSpec

import java.io.{ BufferedWriter, FileWriter, IOException }
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

class ConfigTest extends AnyFlatSpec {

  "Config reader" should "skip non existing default config" in {
    val userHome = Paths.get("/tmp/grafex/.config/grafex")

    deleteTempConfigs(userHome).use(_ =>
      IO {
        Config.read(createContext(Nil))().value.unsafeRunSync() match {
          case Left(err)  => fail(s"Unexpected error happened: $err")
          case Right(res) => assert(res.accountId == "test")
        }
      }
    )
  }

  it should "read existing default config" in {
    val userHome = Paths.get("/tmp/grafex/.config/grafex")
    val configPath = userHome.resolve("grafex.conf")

    val resource = for {
      _    <- deleteTempConfigs(userHome)
      path <- createTempConfigs(userHome, configPath)
      f <- Resource.fromAutoCloseable {
        IO.delay {
          val writer = new BufferedWriter(new FileWriter(path.toFile))
          writer.write("""account-id: "new-test" """)
          writer.flush()
          writer
        }
      }
    } yield f

    resource
      .use(_ => {
        Config
          .read(createContext(Nil))(configPath)
          .value
          .map({
            case Left(err)  => fail(s"Unexpected error happened: $err")
            case Right(res) => assert(res.accountId == "new-test")
          })
      })
      .unsafeRunSync()
  }

  def deleteTempConfigs(userHome: Path): Resource[IO, Boolean] = {
    Resource.make(IO { deleteDir(userHome) })(_ => IO { deleteDir(userHome) })
  }

  private def deleteDir(dir: Path) = {
    if (Files.exists(dir)) {
      Files.walkFileTree(
        dir,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.deleteIfExists(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            Files.deleteIfExists(dir)
            FileVisitResult.CONTINUE
          }
        }
      )
      true
    } else {
      false
    }
  }

  def createTempConfigs(userHome: Path, configPath: Path): Resource[IO, Path] = {
    Resource
      .make(IO {
        Files.createDirectories(userHome)
        Files.createFile(configPath)
      })(_ => IO { deleteDir(userHome) })
  }

  def createContext(configPaths: List[Path], userHome: Path = Paths.get("/tmp/grafex")): Startup.Context = {
    Startup.Context.Cli(userHome, configPaths, null, null, null, null, null)
  }
}
