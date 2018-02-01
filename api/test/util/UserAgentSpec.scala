package util

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class UserAgentSpec extends PlaySpec with OneAppPerSuite {

  private[this] val userAgent = new UserAgent()

  "user agent generates valid strings" in {
    userAgent.generate(
      orgKey = "apicollective",
      applicationKey = "apibuilder",
      versionName = "1.2.3",
      generatorKey = "play_client"
    ) must equal("apibuilder 0.13.0 localhost 9000/apicollective/apibuilder/1.2.3/play_client")

    userAgent.generate(
      orgKey = "apicollective",
      applicationKey = "apibuilder",
      versionName = "1:0",
      generatorKey = "play_client"
    ) must equal("apibuilder 0.13.0 localhost 9000/apicollective/apibuilder/1 0/play_client")
  }

}
