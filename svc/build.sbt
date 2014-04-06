name := "apiserver"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  "postgresql"    %  "postgresql"     % "9.1-901.jdbc4",
  "org.scalatest" %% "scalatest" % "2.1.0" % "test"
)

play.Project.playScalaSettings
