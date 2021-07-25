ThisBuild / organization := "dev.vgerasimov.grafex"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.6"

ThisBuild / githubOwner := "wlad031"
ThisBuild / githubRepository := "grafex"

ThisBuild / resolvers ++= Seq(
  "Typesafe".at("https://repo.typesafe.com/typesafe/releases/"),
  "Java.net Maven2 Repository".at("https://download.java.net/maven/2/"),
  "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
) ++
// GitHub repositories belonging to me ("wlad031")
Seq("shapelse", "scorg")
  .map(Resolver.githubPackages("wlad031", _))

ThisBuild / libraryDependencies ++= Seq(
  "org.scalameta" % "semanticdb-scalac_2.13.6" % "4.4.24"
)

ThisBuild / scalacOptions ++= Seq(
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

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "dev.vgerasimov.grafex.core.build",
    libraryDependencies ++= Seq(
        Seq(
          "org.typelevel" %% "cats-core",
          "org.typelevel" %% "cats-effect"
        ).map(_ % "2.2.0"),
        Seq(
          "io.circe" %% "circe-core",
          "io.circe" %% "circe-generic",
          "io.circe" %% "circe-parser"
        ).map(_ % "0.13.0"),
        Seq(
          "org.http4s" %% "http4s-dsl",
          "org.http4s" %% "http4s-circe",
          "org.http4s" %% "http4s-blaze-server",
          "org.http4s" %% "http4s-blaze-client"
        ).map(_ % "0.21.6"),
        Seq(
          "com.github.pureconfig" %% "pureconfig",
          "com.github.pureconfig" %% "pureconfig-cats-effect",
          "com.github.pureconfig" %% "pureconfig-circe"
        ).map(_ % "0.14.0"),
        Seq(
          "ch.qos.logback"    % "logback-classic"   % "1.2.3",
          "io.chrisdavenport" %% "log4cats-slf4j"   % "1.1.1",
          "com.monovore"      %% "decline"          % "1.3.0",
          "com.lihaoyi"       %% "fansi"            % "0.2.7",
          "org.neo4j.driver"  % "neo4j-java-driver" % "4.1.1"
        ),
        Seq(
          "com.dimafeng" %% "neotypes",
          "com.dimafeng" %% "neotypes-cats-effect",
          "com.dimafeng" %% "neotypes-cats-data",
          "com.dimafeng" %% "neotypes-refined"
        ).map(_ % "0.15.1"),
        Seq(
          "org.scalatest" %% "scalatest" % "3.2.2" % "test"
        ),
        Seq(
          "dev.vgerasimov" %% "scorg" % "0.1.0"
        )
      ).flatten
  )

lazy val `describe-mode` = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-modes/describe"))
  .dependsOn(core)

lazy val `graph-mode` = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-modes/graph"))
  .dependsOn(core)

// FIXME: should not depend on describe mode
lazy val `account-mode` = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-modes/account"))
  .dependsOn(core % "test->test;compile->compile", `graph-mode`)

lazy val `datasource-mode` = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-modes/datasource"))
  .dependsOn(core)

lazy val modes = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("grafex-modes"))
  .aggregate(`describe-mode`, `graph-mode`, `account-mode`, `datasource-mode`)

lazy val service = project
  .in(file("grafex-service"))
  .aggregate(core.jvm, modes.jvm)
  .dependsOn(
    core.jvm              % "test->test;compile->compile",
    modes.jvm             % "test->test;compile->compile",
    `describe-mode`.jvm   % "test->test;compile->compile",
    `graph-mode`.jvm      % "test->test;compile->compile",
    `account-mode`.jvm    % "test->test;compile->compile",
    `datasource-mode`.jvm % "test->test;compile->compile"
  )
  .settings(
    name := "grafex-service",
    assembly / mainClass := Some("dev.vgerasimov.grafex.ServiceMain"),
    assembly / assemblyJarName := "grafex-service.jar"
  )

lazy val `webui-server`: Project = project
  .in(file("grafex-webui/server"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(
    core.js              % "test->test;compile->compile",
    modes.js             % "test->test;compile->compile",
    `describe-mode`.js   % "test->test;compile->compile",
    `graph-mode`.js      % "test->test;compile->compile",
    `account-mode`.js    % "test->test;compile->compile",
    `datasource-mode`.js % "test->test;compile->compile"
  )
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
        "com.github.japgolly.scalajs-react" %%% "core" % "2.0.0-RC1",
        "com.github.japgolly.scalacss" %%% "ext-react" % "0.7.0"
      ),
    Compile / npmDependencies ++= Seq(
        "react"     -> "17.0.2",
        "react-dom" -> "17.0.2"
      )
  )

lazy val `webui-client` = project
  .in(file("grafex-webui/client"))
  .settings(
    scalaJSProjects := Seq(`webui-server`),
    Assets / pipelineStages := Seq(scalaJSPipeline)
  )
  .enablePlugins(WebScalaJSBundlerPlugin)

lazy val webui = project
  .in(file("grafex-webui"))
  .aggregate(`webui-server`, `webui-client`)

lazy val root = project
  .in(file("."))
  .aggregate(
    service,
    webui,
    `webui-server`
  )

lazy val docs = project
  .in(file("grafex-docs"))
  .dependsOn(root)
  .enablePlugins(MdocPlugin)

Test / testOptions += Tests.Argument(
  framework = Some(TestFrameworks.ScalaTest),
  args = List("-oSD")
)

//Compile /scalacOptions ++= Seq("-groups")
//doc /scalacOptions ++= Seq("-groups")
