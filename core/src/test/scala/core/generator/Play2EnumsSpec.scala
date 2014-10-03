package core.generator

import com.gilt.apidocgenerator.models.ServiceDescription
import core.{ServiceDescriptionBuilder, TestHelper}
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2EnumsSpec extends FunSpec with ShouldMatchers {

  describe("for a model with 2 enum fields") {

    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "enums": {
        "age_group": {
          "values": [
            { "name": "twenties" },
            { "name": "thirties" }
          ]
        },
        "genre": {
          "values": [
            { "name": "Classical" },
            { "name": "Jazz" }
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "age_group" },
            { "name": "music", "type": "genre" }
          ]
        }
      }
    }
    """

    val ssd = new ScalaServiceDescription(ServiceDescriptionBuilder(json))

    it("generates valid models") {
      val enums = ssd.enums.map(Play2Enums.build(_)).mkString("\n\n")
      TestHelper.assertEqualsFile("core/src/test/resources/play2enums-example.txt", enums)
    }

    it("generates valid json conversions") {
      val jsonConversions = ssd.enums.map(Play2Enums.buildJson("Test", _)).mkString("\n\n")
      TestHelper.assertEqualsFile("core/src/test/resources/play2enums-json-example.txt", jsonConversions)
    }
  }


}
