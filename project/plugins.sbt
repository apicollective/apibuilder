// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.11")

addSbtPlugin("com.gilt.sbt" % "sbt-newrelic" % "0.1.18")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.1")
