name := "apidoc"

scalaVersion in ThisBuild := "2.11.1"

lazy val core = project
  .in(file("core"))
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.0"
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(commonPlaySettings: _*)
  .settings(
    version := "1.0-SNAPSHOT"
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(commonPlaySettings: _*)
  .settings(
    version := "1.0-SNAPSHOT"
  )

// commenting out for now while we get 2.3 working
//lazy val sbtGenerator = project
//  .in(file("sbt-apigen"))
//  .dependsOn(core)
//  .aggregate(core)
//  .settings(commonSettings: _*)
//  .settings(
//    version := "1.0-SNAPSHOT",
//    name := "sbt-apigen",
//    sbtPlugin := true,
//    description := """SBT plugin to generate Scala client code"""
//  )

lazy val commonPlaySettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    jdbc,
    anorm,
    ws,
    "postgresql" % "postgresql" % "9.3-1101.jdbc4"
  )
)

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.1",
    "org.scalatest" %% "scalatest" % "2.1.7" % "test"
  ),
  scalacOptions += "-feature"
)
