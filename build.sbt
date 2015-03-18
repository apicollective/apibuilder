import play.PlayImport.PlayKeys._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name := "apidoc"

organization := "com.gilt.apidoc"

scalaVersion in ThisBuild := "2.11.5"

// required because of issue between scoverage & sbt
parallelExecution in Test in ThisBuild := true

lazy val lib = project
  .in(file("lib"))
  .settings(commonSettings: _*)

val avroVersion = "1.7.7"

lazy val avro = project
  .in(file("avro"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.avro"   % "avro"              % avroVersion,
      "org.apache.avro"   % "avro-compiler"     % avroVersion,
      "com.typesafe.play" %% "play-json" % "2.3.8",
      "org.scalatest"     %% "scalatest" % "2.2.0" % "test"
    )
  )

lazy val core = project
  .in(file("core"))
  .dependsOn(generated, lib, avro)
  .aggregate(generated, lib, avro)
  .settings(commonSettings: _*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.8"
    )
  )

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
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
    routesImport += "com.gilt.apidoc.api.v0.Bindables._",
    libraryDependencies ++= Seq(
      ws,
      jdbc,
      anorm,
      "org.postgresql" % "postgresql"    % "9.3-1101-jdbc4",
      "org.mindrot"    % "jbcrypt"       % "0.3m",
      "com.sendgrid"   % "sendgrid-java" % "2.1.0",
      "org.scalatestplus" %% "play" % "1.2.0" % "test"
    )
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(generated, lib)
  .aggregate(generated, lib)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.gilt.apidoc.api.v0.Bindables._",
    libraryDependencies ++= Seq(
      "com.github.tototoshi" %% "scala-csv" % "1.2.0"
    )
  )

lazy val spec = project
  .in(file("spec"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatestplus" %% "play" % "1.2.0" % "test"
    )
  )


lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  organization := "com.gilt.apidoc",
  libraryDependencies ++= Seq(
    "org.atteo" % "evo-inflector" % "1.2.1",
    "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  ),
  scalacOptions += "-feature",
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
  homepage := Some(url("https://github.com/gilt/apidoc")),
  pomExtra := (
  <scm>
    <url>https://github.com/gilt/apidoc.git</url>
    <connection>scm:git:git@github.com:gilt/apidoc.git</connection>
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
