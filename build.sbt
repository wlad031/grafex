ThisBuild / organization := "com.grafex"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.4"

resolvers ++= Seq(
  "Typesafe".at("https://repo.typesafe.com/typesafe/releases/"),
  "Java.net Maven2 Repository".at("https://download.java.net/maven/2/"),
  "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
)

libraryDependencies ++= Seq(
  Seq(
    "com.chuusai" %% "shapeless" % "2.4.0-M1"
  ),
  Seq(
    "eu.timepit" %% "refined",
    "eu.timepit" %% "refined-cats",
    "eu.timepit" %% "refined-pureconfig"
  ).map(_ % "0.9.17"),
  Seq(
    "org.typelevel" %% "cats-core",
    "org.typelevel" %% "cats-effect"
  ).map(_ % "2.2.0"),
  Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-shapes",
    "io.circe" %% "circe-literal"
  ).map(_ % "0.12.3"),
  Seq(
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-blaze-client"
  ).map(_ % "0.21.6"),
  Seq(
    "com.github.pureconfig" %% "pureconfig",
    "com.github.pureconfig" %% "pureconfig-cats-effect",
    "com.github.pureconfig" %% "pureconfig-circe",
  ).map(_ % "0.14.0"),
  Seq(
    "ch.qos.logback"           % "logback-classic"   % "1.2.3",
    "io.chrisdavenport"        %% "log4cats-slf4j"   % "1.1.1",
    "com.monovore"             %% "decline"          % "1.3.0",
    "com.lihaoyi"              %% "fansi"            % "0.2.7",
    "org.neo4j.driver"         % "neo4j-java-driver" % "4.1.1",
    "com.github.nikita-volkov" % "sext"              % "0.2.4"
  ),
  Seq(
    "com.beachape" %% "enumeratum",
    "com.beachape" %% "enumeratum-circe"
  ).map(_ % "1.6.1"),
  Seq(
    "com.dimafeng" %% "neotypes",
    "com.dimafeng" %% "neotypes-cats-effect",
    "com.dimafeng" %% "neotypes-cats-data",
    "com.dimafeng" %% "neotypes-refined"
  ).map(_ % "0.15.1"),
  Seq(
    "org.scalatest" %% "scalatest" % "3.2.2" % "test"
  )
).flatten

scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-Xlint:implicit-recursion",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

lazy val root = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(name := "grafex")
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.grafex.build"
  )
  .settings(
    mainClass in assembly := Some("com.grafex.Main")
  )
  .settings(
    assemblyJarName in assembly := "utils.jar"
  )

Test / testOptions += Tests.Argument(
  framework = Some(TestFrameworks.ScalaTest),
  args = List("-oSD")
)
