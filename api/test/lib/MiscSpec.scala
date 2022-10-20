package lib

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class MiscSpec extends PlaySpec with GuiceOneAppPerSuite {

  "isValidEmail" in {
    Misc.isValidEmail("") must be(false)
    Misc.isValidEmail("@") must be(false)
    Misc.isValidEmail("foo@") must be(false)
    Misc.isValidEmail("@bryzek.com") must be(false)
    Misc.isValidEmail("foo@apibuilder.io") must be(true)
  }

  "emailDomain" in {
    Misc.emailDomain("") must be(None)
    Misc.emailDomain("@") must be(None)
    Misc.emailDomain("foo@") must be(None)
    Misc.emailDomain("foo@apibuilder.io") must be(Some("apibuilder.io"))
    Misc.emailDomain("FOO@apibuilder.io") must be(Some("apibuilder.io"))
    Misc.emailDomain("  FOO@apibuilder.io  ") must be(Some("apibuilder.io"))
    Misc.emailDomain("mb@bryzek.com") must be(Some("bryzek.com"))
    Misc.emailDomain("mb@internal.bryzek.com") must be(Some("internal.bryzek.com"))
    Misc.emailDomain("mb") must be(None)
  }

}
