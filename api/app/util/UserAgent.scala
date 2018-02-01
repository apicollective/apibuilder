package util

import lib.{AppConfig, Config}

/**
  * Generates safe user agents
  */
class UserAgent() {

  private[this] val Prefixes = Seq("http://", "https://")
  private[this] lazy val apibuilderVersion = Config.requiredString("git.version")

  def generate(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ): String = {
    Seq(
      "apibuilder",
      apibuilderVersion,
      Seq(
        AppConfig.apibuilderWwwHost,
        orgKey,
        applicationKey,
        versionName,
        generatorKey
      ).map(format).mkString("/")
    ).map(format).mkString(" ")
  }

  def format(value: String): String = {
    Prefixes.foldLeft(value) { case (v, prefix) =>
      val lower = v.toLowerCase()
      if (lower.startsWith(prefix)) {
        format(v.substring(prefix.length))
      } else {
        v
      }
    }.replaceAll(":", " ").replaceAll("\\s+", " ").trim
  }

}
