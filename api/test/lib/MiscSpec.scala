package lib

import org.scalatest.{FunSpec, ShouldMatchers}

class MiscSpec extends FunSpec with ShouldMatchers {

  it("isValidEmail") {
    Misc.isValidEmail("") should be(false)
    Misc.isValidEmail("@") should be(false)
    Misc.isValidEmail("foo@") should be(false)
    Misc.isValidEmail("@gilt.com") should be(false)
    Misc.isValidEmail("foo@apidoc.me") should be(true)
  }

  it("emailDomain") {
    Misc.emailDomain("") should be(None)
    Misc.emailDomain("@") should be(None)
    Misc.emailDomain("foo@") should be(None)
    Misc.emailDomain("foo@apidoc.me") should be(Some("apidoc.me"))
    Misc.emailDomain("FOO@APIDOC.ME") should be(Some("apidoc.me"))
    Misc.emailDomain("  FOO@APIDOC.ME  ") should be(Some("apidoc.me"))
    Misc.emailDomain("mb@gilt.com") should be(Some("gilt.com"))
    Misc.emailDomain("mb@internal.gilt.com") should be(Some("internal.gilt.com"))
    Misc.emailDomain("mb") should be(None)
  }

}
