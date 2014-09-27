name := "apidoc"

scalaVersion in ThisBuild := "2.11.2"

lazy val core = project
  .in(file("core"))
  .settings(commonSettings: _*)
  .settings(
    resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.4"
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(core)
  .aggregate(core)
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
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("apidoc-" + _),
  libraryDependencies ++= Seq(
    "org.atteo" % "evo-inflector" % "1.2.1",
    "org.scalatest" %% "scalatest" % "2.2.0" % "test"
  ),
  scalacOptions += "-feature"
) ++ instrumentSettings ++ Seq(ScoverageKeys.highlighting := true)



