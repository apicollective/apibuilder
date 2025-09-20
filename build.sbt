name := "apibuilder"
organization := "io.apibuilder"

ThisBuild / scalaVersion := "3.7.3"

val playJsonVersion = "2.10.6"

val avroVersion = "1.11.1"

lazy val allScalacOptions = Seq(
  "-feature"
)

lazy val resolversSettings = Seq(
  resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "jitpack" at "https://jitpack.io",
)

lazy val lib = project
  .in(file("lib"))
  .settings(commonSettings*)
  .settings(resolversSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "joda-time" % "joda-time" % "2.14.0",
    )
  )

lazy val avro = project
  .in(file("avro"))
  .dependsOn(lib % "compile->compile;test->test")
  .aggregate(lib)
  .settings(
    scalacOptions ++= allScalacOptions,
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    Test / javaOptions ++= Seq(
      "--add-exports=java.base/sun.security.x509=ALL-UNNAMED",
      "--add-opens=java.base/sun.security.ssl=ALL-UNNAMED"
    )
  )

lazy val swagger = project
  .in(file("swagger"))
  .dependsOn(lib % "compile->compile;test->test")
  .aggregate(lib)
  .settings(
    scalacOptions ++= allScalacOptions,
    libraryDependencies ++= Seq(
      "io.swagger" % "swagger-parser" % "1.0.61",
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
  .enablePlugins(PlayScala)
  .dependsOn(generated, lib, avro, swagger)
  .aggregate(generated, lib, avro, swagger)
  .settings(commonSettings*)
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    ),
  )

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(commonSettings*)
  .settings(resolversSettings)
  .settings(
    scalacOptions ++= Seq("-deprecation:false"),
    libraryDependencies ++= Seq(
      ws,
      jdbc,
      "com.github.mbryzek" % "lib-query" % "0.0.5",
      "com.github.mbryzek" % "lib-util" % "0.0.7",
      "joda-time" % "joda-time" % "2.14.0",
      "org.playframework.anorm" %% "anorm-postgres" % "2.7.0",
      "org.postgresql" % "postgresql" % "42.7.7",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated, core % "compile->compile;test->test")
  .aggregate(generated, core)
  .enablePlugins(PlayScala, JavaAgent)
  .settings(commonSettings*)
  .settings(
  scalacOptions ++= Seq("-deprecation:false"),
    scalacOptions ++= allScalacOptions,
    PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value)),
    PlayKeys.playDefaultPort := 9001,
    testOptions += Tests.Argument("-oF"),
    javaAgents += "com.datadoghq" % "dd-java-agent" % "1.53.0",
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesImport += "io.apibuilder.common.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      filters,
      jdbc,
      ws,
      "com.typesafe.play" %% "play-guice" % "2.9.4",
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "org.projectlombok" % "lombok" % "1.18.42" % "provided",
      ("com.github.mbryzek" % "lib-cipher" % "0.0.7").cross(CrossVersion.for3Use2_13),
      "com.github.mbryzek" % "lib-util" % "0.0.7",
      "com.sendgrid"      %  "sendgrid-java"  % "4.10.3",
      "com.github.mbryzek" % "lib-query" % "0.0.2",
      "com.rollbar" % "rollbar-java" % "2.0.0",
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
    PlayKeys.playDefaultPort := 9000,
    javaAgents += "com.datadoghq" % "dd-java-agent" % "1.53.0",
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesImport += "io.apibuilder.common.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      guice,
      "com.google.inject" % "guice" % "5.1.0",
      "com.google.inject.extensions" % "guice-assistedinject" % "5.1.0",
      "org.projectlombok" % "lombok" % "1.18.42" % "provided",
      "org.apache.commons" % "commons-compress" % "1.28.0",
      "com.github.tototoshi" %% "scala-csv" % "1.4.0",
      "com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
      "org.webjars" %% "webjars-play" % "3.0.1",
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
    "org.atteo" % "evo-inflector" % "1.3",
    "org.typelevel" %% "cats-core" % "2.12.0",
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
