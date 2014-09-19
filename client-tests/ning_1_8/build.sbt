name := "apidoc-standalone"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  json,
  "com.ning" % "async-http-client" % "1.8.13"
)
