package lib

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class UtilSpec extends PlaySpec with OneAppPerSuite {

  "validateReturnUrl for invalid domains" in {
    Util.validateReturnUrl("") must be(Left(Seq("Redirect URL[] must start with / or a known domain")))
    Util.validateReturnUrl("https://google.com/foo") must be(Left(Seq("Redirect URL[https://google.com/foo] must start with / or a known domain")))
  }

  "validateReturnUrl for valid domains" in {
    Util.validateReturnUrl("/") must be(Right("/"))
    Util.validateReturnUrl("https://www.apibuilder.io") must be(Right("/"))
    Util.validateReturnUrl("https://www.apibuilder.io/") must be(Right("/"))
    Util.validateReturnUrl("https://app.apibuilder.io") must be(Right("/"))
    Util.validateReturnUrl("https://app.apibuilder.io/") must be(Right("/"))
  }

}
