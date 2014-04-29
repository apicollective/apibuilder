name := "apidoc-core"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.1.0" % "test"
)

play.Project.playScalaSettings

fork := true

javaOptions in Test ++= Seq(
  s"""-Dirishub.api.token=${sys.props.getOrElse("irishub.api.token", "")}""",
  s"""-Dirishub.api.url=${sys.props.getOrElse("irishub.api.url", "")}"""
)

