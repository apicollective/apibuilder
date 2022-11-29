package core

import helpers.ValidatedTestHelpers
import io.apibuilder.spec.v0.models.{Contact, Info, License}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class InfoSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

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
      expectValid {
        TestHelper.serviceValidatorFromApiJson(json)
      }.info should be(Info(license = None, contact = None))
    }

    it("contact") {
      val json = baseJson.format("""
        "contact": {
          "name": "Foo",
          "email": "Foo@test.apibuilder.me",
          "url": "https://www.apibuilder.io"
        }
      """)

      val contact = Contact(
        name = Some("Foo"),
        email = Some("Foo@test.apibuilder.me"),
        url = Some("https://www.apibuilder.io")
      )
      expectValid {
        TestHelper.serviceValidatorFromApiJson(json)
      }.info should be(Info(contact = Some(contact)))
    }

    it("license") {
      val json = baseJson.format("""
        "license": {
          "name": "MIT",
          "url": "https://opensource.org/licenses/MIT"
        }
      """)

      expectValid {
        TestHelper.serviceValidatorFromApiJson(json)
      }.info should be(Info(license = Some(
        License(
          name = "MIT",
          url = Some("https://opensource.org/licenses/MIT")
        )
      )))
    }

    it("validates license requires name") {
      val json = baseJson.format("""
        "license": {
          "url": "https://opensource.org/licenses/MIT"
        }
      """)
      expectInvalid {
        TestHelper.serviceValidatorFromApiJson(json)
      } should be(Seq("License must have a name"))
    }

  }
}
