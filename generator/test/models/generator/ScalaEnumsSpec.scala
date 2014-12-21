package generator

import com.gilt.apidocspec.models.Service
import models.TestHelper
import core.ServiceBuilder
import org.scalatest.{ ShouldMatchers, FunSpec }

class ScalaEnumsSpec extends FunSpec with ShouldMatchers {

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

    val ssd = new ScalaService(ServiceBuilder(json))

    it("generates valid models") {
      val enums = ssd.enums.map(ScalaEnums.build(_)).mkString("\n\n")
      TestHelper.assertEqualsFile("test/resources/play2enums-example.txt", enums)
    }

    it("generates valid json conversions") {
      val jsonConversions = ssd.enums.map(ScalaEnums.buildJson("Test", _)).mkString("\n\n")
      TestHelper.assertEqualsFile("test/resources/play2enums-json-example.txt", jsonConversions)
    }
  }


}
