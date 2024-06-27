package lib

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class UtilSpec extends PlaySpec with GuiceOneAppPerSuite {

  private[this] val util = app.injector.instanceOf[Util]

  "validateReturnUrl for invalid domains" in {
    util.validateReturnUrl("") must be(Left(Seq("Redirect URL[] must start with / or a known domain")))
    util.validateReturnUrl("https://google.com/foo") must be(Left(Seq("Redirect URL[https://google.com/foo] must start with / or a known domain")))
  }

  "validateReturnUrl for valid domains" in {
    util.validateReturnUrl("/") must be(Right("/"))
    util.validateReturnUrl("https://www.apibuilder.io") must be(Right("/"))
    util.validateReturnUrl("https://www.apibuilder.io/") must be(Right("/"))
    util.validateReturnUrl("https://app.apibuilder.io") must be(Right("/"))
    util.validateReturnUrl("https://app.apibuilder.io/") must be(Right("/"))
  }

}
