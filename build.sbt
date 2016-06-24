import play.PlayImport.PlayKeys._
import play.sbt.PlayImport._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name := "apidoc"

organization := "com.bryzek.apidoc"

scalaVersion in ThisBuild := "2.11.8"

// required because of issue between scoverage & sbt
parallelExecution in Test in ThisBuild := true

lazy val lib = project
  .in(file("lib"))
  .settings(commonSettings: _*)

val avroVersion = "1.8.1"

lazy val avro = project
  .in(file("avro"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "com.typesafe.play" %% "play-json" % "2.4.6",
      "org.scalatest"     %% "scalatest" % "2.2.6" % "test"
    )
  )

lazy val swagger = project
  .in(file("swagger"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .settings(
    libraryDependencies ++= Seq(
      "io.swagger" % "swagger-parser" % "1.0.5",
      "com.typesafe.play" %% "play-json" % "2.4.6",
      "org.scalatest"     %% "scalatest" % "2.2.6" % "test"
    )
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(generated, lib, avro, swagger)
  .aggregate(generated, lib)
  .settings(commonSettings: _*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.4.6"
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
  .dependsOn(generated, core)
  .aggregate(generated, core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.bryzek.apidoc.api.v0.Bindables._",
    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      ws,
      jdbc,
      "com.typesafe.play" %% "anorm"         % "2.5.0",
      "org.postgresql"    %  "postgresql"    % "9.4.1208",
      "org.mindrot"       %  "jbcrypt"       % "0.3m",
      "com.sendgrid"      %  "sendgrid-java" % "3.0.0",
      specs2              %  Test,
      "org.scalatestplus" %% "play" % "1.4.0" % "test"
    )
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.bryzek.apidoc.api.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-compress" % "1.11",
      "com.github.tototoshi" %% "scala-csv" % "1.3.3",
      "org.pegdown" % "pegdown" % "1.6.0"
    )
  )

lazy val spec = project
  .in(file("spec"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatestplus" %% "play" % "1.4.0" % "test"
    )
  )


lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  organization := "com.bryzek.apidoc",
  libraryDependencies ++= Seq(
    "org.atteo" % "evo-inflector" % "1.2.1",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  ),
  scalacOptions += "-feature",
  sources in (Compile,doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false,
  coverageHighlighting := true
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
  homepage := Some(url("https://github.com/mbryzek/apidoc")),
  pomExtra := (
  <scm>
    <url>https://github.com/mbryzek/apidoc.git</url>
    <connection>scm:git:git@github.com:mbryzek/apidoc.git</connection>
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
version := "0.11.29"
