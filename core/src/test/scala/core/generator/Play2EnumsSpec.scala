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
      val targetPath = "core/src/test/files/play2enums-example.txt"
      val enums = Play2Enums.build(service.models.head).get
      println(enums)

      if (enums.trim != TestHelper.readFile(targetPath).trim) {
        val tmpPath = s"$targetPath.tmp"
        TestHelper.writeToFile(tmpPath, enums.trim)
        fail(s"Did not generate expected scala code for models. diff $targetPath $tmpPath")
      }
    }

    it("generates valid json conversions") {
      val jsonConversions = Play2Enums.buildJson("Test", service.models.head).get
      println(jsonConversions)
      jsonConversions.trim should be(TestHelper.readFile("core/src/test/files/play2enums-json-example.txt").trim)
    }
  }


}
