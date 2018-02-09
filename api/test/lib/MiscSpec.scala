package lib

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class MiscSpec extends PlaySpec with OneAppPerSuite {

  it("isValidEmail") {
    Misc.isValidEmail("") must be(false)
    Misc.isValidEmail("@") must be(false)
    Misc.isValidEmail("foo@") must be(false)
    Misc.isValidEmail("@bryzek.com") must be(false)
    Misc.isValidEmail("foo@apidoc.me") must be(true)
  }

  it("emailDomain") {
    Misc.emailDomain("") must be(None)
    Misc.emailDomain("@") must be(None)
    Misc.emailDomain("foo@") must be(None)
    Misc.emailDomain("foo@apidoc.me") must be(Some("apidoc.me"))
    Misc.emailDomain("FOO@APIDOC.ME") must be(Some("apidoc.me"))
    Misc.emailDomain("  FOO@APIDOC.ME  ") must be(Some("apidoc.me"))
    Misc.emailDomain("mb@bryzek.com") must be(Some("bryzek.com"))
    Misc.emailDomain("mb@internal.bryzek.com") must be(Some("internal.bryzek.com"))
    Misc.emailDomain("mb") must be(None)
  }

}
