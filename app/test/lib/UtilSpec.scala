package lib

import org.scalatest.{FunSpec, Matchers}

class UtilSpec extends FunSpec with Matchers {

  it("validateReturnUrl for invalid domains") {
    Util.validateReturnUrl("") should be(Left(Seq("Redirect URL[] must start with / or a known domain")))
    Util.validateReturnUrl("https://google.com/foo") should be(Left(Seq("Redirect URL[https://google.com/foo] must start with / or a known domain")))
  }

  it("validateReturnUrl for valid domains") {
    Util.validateReturnUrl("/") should be(Right("/"))
    Util.validateReturnUrl("https://www.apibuilder.io") should be(Right("/"))
    Util.validateReturnUrl("https://www.apibuilder.io/") should be(Right("/"))
    Util.validateReturnUrl("https://app.apibuilder.io") should be(Right("/"))
    Util.validateReturnUrl("https://app.apibuilder.io/") should be(Right("/"))
  }

}
