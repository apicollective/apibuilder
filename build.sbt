import play.PlayImport.PlayKeys._

name := "apidoc"

organization := "com.gilt.apidoc"

scalaVersion in ThisBuild := "2.11.2"

lazy val core = project
  .in(file("core"))
  .settings(commonSettings: _*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.5"
    )
  )

lazy val generated = project
  .in(file("generated"))
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "com.gilt.apidoc.Bindables._",
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      "org.postgresql" % "postgresql" % "9.3-1101-jdbc4",
      "org.mindrot"    %  "jbcrypt"   % "0.3m"
    )
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  organization := "com.gilt.apidoc",
  libraryDependencies ++= Seq(
    "org.atteo" % "evo-inflector" % "1.2.1",
    "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  ),
  scalacOptions += "-feature",
  ScoverageKeys.highlighting := true
) ++ instrumentSettings ++ publishSettings

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
