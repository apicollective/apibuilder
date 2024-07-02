import play.sbt.PlayScala._

name := "apibuilder"

organization := "io.apibuilder"

ThisBuild / scalaVersion := "3.4.2"

lazy val allScalacOptions = Seq(
  "-feature",
  "-Xfatal-warnings"
)

lazy val lib = project
  .in(file("lib"))
  .settings(commonSettings*)

val avroVersion = "1.11.1"

val playJsonVersion = "2.10.6"

lazy val avro = project
  .in(file("avro"))
  .dependsOn(generated, lib % "compile->compile;test->test")
  .aggregate(generated, lib)
  .settings(
    scalacOptions ++= allScalacOptions,
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "com.typesafe.play" %% "play-json"        % playJsonVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    Test / javaOptions ++= Seq(
      "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED"
    )
  )

lazy val swagger = project
  .in(file("swagger"))
  .dependsOn(generated, lib % "compile->compile;test->test")
  .aggregate(generated, lib)
  .settings(
    scalacOptions ++= allScalacOptions,
    libraryDependencies ++= Seq(
      "io.swagger" % "swagger-parser" % "1.0.61",
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    Test / javaOptions ++= Seq(
      "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED"
    )
  )

val circeVersion = "0.14.9"
lazy val core = project
  .in(file("core"))
  .dependsOn(generated, lib, avro, swagger)
  .aggregate(generated, lib)
  .settings(commonSettings*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "https://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )
  )

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated, core % "compile->compile;test->test")
  .aggregate(generated, core)
  .enablePlugins(PlayScala, JavaAgent)
  .settings(commonSettings*)
  .settings(
    scalacOptions ++= allScalacOptions,
    PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value)),
    testOptions += Tests.Argument("-oF"),
    javaAgents += "com.datadoghq" % "dd-java-agent" % "1.8.0",
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesImport += "io.apibuilder.common.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      filters,
      jdbc,
      ws,
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "org.projectlombok" % "lombok" % "1.18.32" % "provided",
      "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.postgresql"    %  "postgresql"     % "42.7.3",
      "org.mindrot"       %  "jbcrypt"        % "0.4",
      "com.sendgrid"      %  "sendgrid-java"  % "4.10.2",
      "com.github.mbryzek" % "lib-query" % "0.0.1",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "com.github.tomakehurst" % "wiremock-standalone" % "3.0.1" % Test
    ),
    bashScriptExtraDefines ++= Seq(
      """addJava "--add-opens=java.base/java.lang=ALL-UNNAMED""""
    ),
    Test / javaOptions ++= Seq(
      "-Dconfig.resource=application.test.conf"
    )
  )

lazy val app = project
  .in(file("app"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala, JavaAgent, SbtTwirl)
  .settings(commonSettings*)
  .settings(
    scalacOptions ++= allScalacOptions,
    PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value)),
    javaAgents += "com.datadoghq" % "dd-java-agent" % "1.8.0",
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesImport += "io.apibuilder.common.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      guice,
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "org.projectlombok" % "lombok" % "1.18.28" % "provided",
      "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.apache.commons" % "commons-compress" % "1.26.2",
      "com.github.tototoshi" %% "scala-csv" % "1.4.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "org.webjars" %% "webjars-play" % "2.8.18",
      "org.webjars" % "bootstrap" % "3.3.7",
      "org.webjars" % "bootstrap-social" % "5.0.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    bashScriptExtraDefines ++= Seq(
      """addJava "--add-opens=java.base/java.lang=ALL-UNNAMED""""
    ),
    Test / javaOptions ++= Seq(
      "-Dconfig.resource=application.conf"
    )
  )

lazy val spec = project
  .in(file("spec"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("apibuilder-" + _),
  organization := "io.apibuilder",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
    "org.atteo" % "evo-inflector" % "1.3",
    "org.typelevel" %% "cats-core" % "2.12.0",
    "org.slf4j" % "slf4j-api" % "2.0.13",
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
  ),
  scalacOptions ++= allScalacOptions,
  Test / javaOptions ++= Seq(
    "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED",
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
  ),
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / publishArtifact := false
) ++ publishSettings

lazy val publishSettings: Seq[Setting[_]] = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.html")),
  homepage := Some(url("https://github.com/apicollective/apibuilder")),
  pomExtra := (
  <scm>
    <url>https://github.com/apicollective/apibuilder.git</url>
    <connection>scm:git:git@github.com:apicollective/apibuilder.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mbryzek</id>
      <name>Michael Bryzek</name>
      <url>https://github.com/mbryzek</url>
    </developer>
  </developers>
  )
)
version := "0.16.19"
