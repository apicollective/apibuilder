import play.sbt.PlayScala._

name := "apibuilder"

organization := "io.apibuilder"

scalaVersion in ThisBuild := "2.13.2"

lazy val lib = project
  .in(file("lib"))
  .settings(commonSettings: _*)

val avroVersion = "1.8.2"

val playJsonVersion = "2.8.1"

lazy val avro = project
  .in(file("avro"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "com.typesafe.play" %% "play-json"        % playJsonVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    )
  )

lazy val swagger = project
  .in(file("swagger"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .settings(
    libraryDependencies ++= Seq(
      "io.swagger" % "swagger-parser" % "1.0.50",
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
   )
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(generated, lib, avro, swagger)
  .aggregate(generated, lib)
  .settings(commonSettings: _*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "https://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % playJsonVersion
    )
  )

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated, core % "compile->compile;test->test")
  .aggregate(generated, core)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .settings(commonSettings: _*)
  .settings(
    testOptions += Tests.Argument("-oF"),
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Artifactory" at "https://flow.jfrog.io/flow/libs-release",
    libraryDependencies ++= Seq(
      filters,
      guice,
      jdbc,
      ws,
      "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.postgresql"    %  "postgresql"     % "42.2.1",
      "org.mindrot"       %  "jbcrypt"        % "0.4",
      "com.sendgrid"      %  "sendgrid-java"  % "4.1.2",
      "io.flow"           %% "lib-postgresql-play-play28" % "0.3.63",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
      "com.github.tomakehurst" % "wiremock-standalone" % "2.20.0" % Test
    )
  )

lazy val app = project
  .in(file("app"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "io.apibuilder.api.v0.Bindables.Core._",
    routesImport += "io.apibuilder.api.v0.Bindables.Models._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.apache.commons" % "commons-compress" % "1.16.1",
      "com.github.tototoshi" %% "scala-csv" % "1.3.6",
      "org.pegdown" % "pegdown" % "1.6.0",
      "org.webjars" %% "webjars-play" % "2.8.0",
      "org.webjars" % "bootstrap" % "3.3.7",
      "org.webjars" % "bootstrap-social" % "5.0.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    )
  )

lazy val spec = project
  .in(file("spec"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    )
  )


lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("apibuilder-" + _),
  organization := "io.apibuilder",
  libraryDependencies ++= Seq(
    "org.atteo" % "evo-inflector" % "1.2.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  ),
  scalacOptions += "-feature",
  sources in (Compile,doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false
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
  publishArtifact in Test := false,
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

publishSettings
version := "0.15.2"
