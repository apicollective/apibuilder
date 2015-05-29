package core

import com.gilt.apidoc.spec.v0.models.{Contact, Info, License}
import lib.VersionTag
import org.scalatest.{FunSpec, Matchers}

class InfoSpec extends FunSpec with Matchers {

  it("migrates legacy description to info object") {
    val json = """
    {
      "name": "Api Doc",
      "description": "foo"
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    validator.service.info.get.description should be(Some("foo"))
  }

  describe("with info") {

    val baseJson = """
    {
      "name": "Api Doc",
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
      validator.errors.mkString("") should be("")
      validator.service.info should be(Some(Info(None, None, None)))
    }

    it("description") {
      val json = baseJson.format(""" "description": "hey" """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      validator.service.info should be(Some(Info(description = Some("hey"))))
    }

    it("contact") {
      val json = baseJson.format("""
        "contact": {
          "name": "Foo",
          "email": "Foo@test.apidoc.me",
          "url": "http://www.apidoc.me"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      val contact = Contact(
        name = Some("Foo"),
        email = Some("Foo@test.apidoc.me"),
        url = Some("http://www.apidoc.me")
      )
      validator.service.info should be(Some(Info(contact = Some(contact))))
    }

    it("license") {
      val json = baseJson.format("""
        "license": {
          "name": "MIT",
          "url": "http://opensource.org/licenses/MIT"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      val license = License(
        name = "MIT",
        url = Some("http://opensource.org/licenses/MIT")
      )
      validator.service.info should be(Some(Info(license = Some(license))))
    }

    it("validates license requires name") {
      val json = baseJson.format("""
        "license": {
          "url": "http://opensource.org/licenses/MIT"
        }
      """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("License must have a name")
    }

  }
}
