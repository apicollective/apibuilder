package util

import javax.inject.Inject
import lib.AppConfig

/**
  * Generates safe user agents
  */
class UserAgent @Inject() (
  appConfig: AppConfig
) {

  private[this] val Prefixes: Seq[String] = Seq("http://", "https://")

  def generate(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ): String = {
    Seq(
      "apibuilder",
      appConfig.apibuilderVersion,
      Seq(
        appConfig.apibuilderWwwHost,
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
