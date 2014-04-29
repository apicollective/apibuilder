import play.Project._

name := "apidoc"

version := "1.0-SNAPSHOT"

playScalaSettings

lazy val apidoc = project.in(file(".")).aggregate(core, svc, web)

lazy val core = project

lazy val svc = project
  .aggregate(core)
  .dependsOn(core)

lazy val web = project
  .aggregate(core)
  .dependsOn(core)

