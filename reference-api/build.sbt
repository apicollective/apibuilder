name := "reference-api"

playScalaSettings

description := "A reference api for testing code generation."

scalacOptions ++= Seq(
  "-feature",
  "-deprecation"
)

libraryDependencies ++= Seq(
  jdbc, anorm,
  "com.h2database" % "h2" % "1.4.178",
  // the version that ships with play 2.2.3 has a buggy dependency on scalacheck
  "org.specs2" %% "specs2" % "2.3.12"
)
