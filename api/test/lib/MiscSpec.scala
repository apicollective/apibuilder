package lib

import helpers.ValidatedTestHelpers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class MiscSpec extends PlaySpec with GuiceOneAppPerSuite with ValidatedTestHelpers {

  "validateEmail" must {
    "invalid" in {
      def setup(value: String): Seq[String] = {
        expectInvalid {
          Misc.validateEmail(value)
        }.map(_.message)
      }

      setup("") mustBe Seq("Email must have an '@' symbol")
      setup("  ") mustBe Seq("Email must have an '@' symbol")
      setup("@") mustBe Seq("Invalid Email: missing username and domain")
      setup("foo@") mustBe Seq("Invalid Email: missing domain")
      setup("@bryzek.com") mustBe Seq("Invalid Email: missing username")
    }

    "valid" in {
      def setup(value: String) = expectValid {
        Misc.validateEmail(value)
      }

      setup("foo@apibuilder.io") mustBe "foo@apibuilder.io"
      setup(" foo@apibuilder.io ") mustBe "foo@apibuilder.io"
    }
  }

  "emailDomain" must {
    "no domain" in {
      def setup(value: String): Option[String] = Misc.emailDomain(value)

      setup("") must be(None)
      setup("@") must be(None)
      setup("foo@") must be(None)
      setup("mb") must be(None)
    }

    "with domain" in {
      def setup(value: String): String = Misc.emailDomain(value).value

      setup("foo@apibuilder.io") must be("apibuilder.io")
      setup("FOO@apibuilder.io") must be("apibuilder.io")
      setup("  FOO@apibuilder.io  ") must be("apibuilder.io")
      setup("mb@bryzek.com") must be("bryzek.com")
      setup("mb@internal.bryzek.com") must be("internal.bryzek.com")
    }
  }

}
