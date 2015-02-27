package lib

import org.scalatest.{FunSpec, ShouldMatchers}

class MiscSpec extends FunSpec with ShouldMatchers {

  it("isValidEmail") {
    Misc.isValidEmail("") should be(false)
    Misc.isValidEmail("@") should be(false)
    Misc.isValidEmail("foo@") should be(false)
    Misc.isValidEmail("foo@apidoc.me") should be(true)
  }

}
