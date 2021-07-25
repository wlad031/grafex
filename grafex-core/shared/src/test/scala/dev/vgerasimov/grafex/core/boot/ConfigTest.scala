package dev.vgerasimov.grafex
package core
package boot

import cats.effect.{ IO, Resource }
import org.scalatest.funsuite.AnyFunSuite

import java.io.{ BufferedWriter, FileWriter, IOException }
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

class ConfigTest extends AnyFunSuite {

  test("Default home config path should point to $HOME/.config/grafex/grafex.conf") {
    val userHomeStr = "/tmp/grafex"
    val userHome = Paths.get(userHomeStr)
    val path = Config.buildHomeConfigPath(createContext(Nil, userHome))
    assert(path === Paths.get(s"$userHomeStr/.config/grafex/grafex.conf"))
  }

  test("Config reader should skip non existing default config") {
    val configDir = Paths.get("/tmp/grafex/.config/grafex")

    deleteTempConfigs(configDir).use(_ =>
      IO {
        Config.load(createContext(Nil))().value.unsafeRunSync() match {
          case Left(err)  => fail(s"Unexpected error happened: $err")
          case Right(res) => assert(res.accountId === "test")
        }
      }
    )
  }

  test("Config reader shoud read existing default config") {
    val configDir = Paths.get("/tmp/grafex/.config/grafex")
    val configPath = configDir.resolve("grafex.conf")

    val resource = for {
      _    <- deleteTempConfigs(configDir)
      path <- createTempConfigs(configDir, configPath)
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
          .load(createContext(Nil))(configPath)
          .value
          .map({
            case Left(err)  => fail(s"Unexpected error happened: $err")
            case Right(res) => assert(res.accountId === "new-test")
          })
      })
      .unsafeRunSync()
  }

  def deleteTempConfigs(configDir: Path): Resource[IO, Boolean] = {
    Resource.make(IO { deleteDir(configDir) })(_ => IO { deleteDir(configDir) })
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

  def createTempConfigs(configDir: Path, configPath: Path): Resource[IO, Path] = {
    Resource
      .make(IO {
        Files.createDirectories(configDir)
        Files.createFile(configPath)
      })(_ => IO { deleteDir(configDir) })
  }

  def createContext(configPaths: List[Path], userHome: Path = Paths.get("/tmp/grafex")): Startup.Context = {
    Startup.Context.Cli(userHome, configPaths, null, null, null, null)
  }
}
