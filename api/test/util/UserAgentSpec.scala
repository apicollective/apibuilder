package util

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class UserAgentSpec extends PlaySpec with OneAppPerSuite {

  private[this] val userAgent = app.injector.instanceOf[UserAgent]

  "user agent generates valid strings" in {
    userAgent.generate(
      orgKey = "apicollective",
      applicationKey = "apibuilder",
      versionName = "1.2.3",
      generatorKey = Some("play_client")
    ) must fullyMatch regex("apibuilder 0\\.[0-9]+\\.[0-9]+ localhost 9000/apicollective/apibuilder/1\\.2\\.3/play_client")

    userAgent.generate(
      orgKey = "apicollective",
      applicationKey = "apibuilder",
      versionName = "1:0",
      generatorKey = Some("play_client")
    ) must fullyMatch regex("apibuilder 0\\.[0-9]+\\.[0-9]+ localhost 9000/apicollective/apibuilder/1 0/play_client")

    userAgent.generate(
      orgKey = "apicollective",
      applicationKey = "apibuilder",
      versionName = "1:0",
      generatorKey = None
    ) must fullyMatch regex("apibuilder 0\\.[0-9]+\\.[0-9]+ localhost 9000/apicollective/apibuilder/1 0")
  }

}
