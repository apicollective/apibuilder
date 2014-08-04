package core.generator

import core.{ TestHelper, ServiceDescription }
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2EnumsSpec extends FunSpec with ShouldMatchers {

  private lazy val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            %s
          ]
        }
      }
    }
    """

  it("enumName") {
    Play2Enums.enumName("UnableToFulfill") should be("UnableToFulfill")
    Play2Enums.enumName("UNABLE_TO_FULFILL") should be("UnableToFulfill")
  }

  describe("for a model without enums") {

    val service = ServiceDescription(json.format("""{ "name": "age_group", "type": "string" }"""))

    it("Generates no models") {
      Play2Enums.build(service.models.head) should be(None)
    }

    it("Generates no json conversions") {
      Play2Enums.buildJson("Test", service.models.head) should be(None)
    }

  }

  describe("for a model with 2 enum fields") {

    val service = ServiceDescription(json.format("""
{ "name": "age_group", "type": "string", "enum": ["twenties", "thirties"] },
{ "name": "party_theme", "type": "string", "enum": ["twenties", "thirties"] }
"""))

    it("generates valid models") {
      val enums = Play2Enums.build(service.models.head).get
      TestHelper.assertEqualsFile("core/src/test/resources/play2enums-example.txt", enums)
    }

    it("generates valid json conversions") {
      val jsonConversions = Play2Enums.buildJson("Test", service.models.head).get
      jsonConversions.trim should be(TestHelper.readFile("core/src/test/resources/play2enums-json-example.txt").trim)
    }
  }


}
