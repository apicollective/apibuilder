name := "apidoc"

playScalaSettings

lazy val core = project
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.2.3"
    ),
    // Temporary addition until api.json is moved.
    unmanagedClasspath in Test += (baseDirectory in ThisBuild).value / "svc"
  )

lazy val svc = project
  .dependsOn(core)
  .aggregate(core)
  .settings(playScalaSettings: _*)
  .settings(commonSettings: _*)
  .settings(commonPlaySettings: _*)
  .settings(
    version := "1.0-SNAPSHOT"
  )

lazy val web = project
  .dependsOn(core)
  .aggregate(core)
  .settings(playScalaSettings: _*)
  .settings(commonSettings: _*)
  .settings(commonPlaySettings: _*)
  .settings(
    version := "1.0-SNAPSHOT"
  )

lazy val sbtGenerator = project
  .in(file("sbt-apigen"))
  .dependsOn(core)
  .aggregate(core)
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    name := "sbt-apigen",
    sbtPlugin := true,
    description := """SBT plugin to generate Scala client code"""
  )

lazy val commonPlaySettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    jdbc,
    anorm,
    "postgresql" % "postgresql" % "9.1-901.jdbc4"
  )
)

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.1.5" % "test"
  )
)
