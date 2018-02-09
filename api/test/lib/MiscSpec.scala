package lib

import org.scalatest.{FunSpec, ShouldMatchers}

class MiscSpec extends FunSpec with ShouldMatchers {

  it("isValidEmail") {
    Misc.isValidEmail("") should be(false)
    Misc.isValidEmail("@") should be(false)
    Misc.isValidEmail("foo@") should be(false)
    Misc.isValidEmail("@bryzek.com") should be(false)
    Misc.isValidEmail("foo@apidoc.me") should be(true)
  }

  it("emailDomain") {
    Misc.emailDomain("") should be(None)
    Misc.emailDomain("@") should be(None)
    Misc.emailDomain("foo@") should be(None)
    Misc.emailDomain("foo@apidoc.me") should be(Some("apidoc.me"))
    Misc.emailDomain("FOO@APIDOC.ME") should be(Some("apidoc.me"))
    Misc.emailDomain("  FOO@APIDOC.ME  ") should be(Some("apidoc.me"))
    Misc.emailDomain("mb@bryzek.com") should be(Some("bryzek.com"))
    Misc.emailDomain("mb@internal.bryzek.com") should be(Some("internal.bryzek.com"))
    Misc.emailDomain("mb") should be(None)
  }

}
