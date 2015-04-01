name := "play_2_3"

version := "0.0.1"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  ws
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
