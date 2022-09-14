package core

import io.apibuilder.spec.v0.models.{Contact, Info, License}
import lib.VersionTag
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InfoSpec extends AnyFunSpec with Matchers {

  describe("with info") {

    val baseJson = """
    {
      "name": "API Builder",
      "apidoc": {
        "version": "0.9.7"
      },
      "info": {
        %s
      }
    }
    """

    it("accepts empty objects") {
      val json = baseJson.format("")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("")
      validator.service().info should be(Info(license = None, contact = None))
    }

    it("contact") {
      val json = baseJson.format("""
        "contact": {
          "name": "Foo",
          "email": "Foo@test.apibuilder.me",
          "url": "http://www.apidoc.me"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("")
      val contact = Contact(
        name = Some("Foo"),
        email = Some("Foo@test.apibuilder.me"),
        url = Some("http://www.apidoc.me")
      )
      validator.service().info should be(Info(contact = Some(contact)))
    }

    it("license") {
      val json = baseJson.format("""
        "license": {
          "name": "MIT",
          "url": "http://opensource.org/licenses/MIT"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("")
      val license = License(
        name = "MIT",
        url = Some("http://opensource.org/licenses/MIT")
      )
      validator.service().info should be(Info(license = Some(license)))
    }

    it("validates license requires name") {
      val json = baseJson.format("""
        "license": {
          "url": "http://opensource.org/licenses/MIT"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString("") should be("License must have a name")
    }

  }
}
