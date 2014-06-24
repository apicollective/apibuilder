package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionEnumSpec extends FunSpec with Matchers {

  it("enum with string type") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "string", "values": ["Twenties", "Thirties"] }
          ]
        }
      }
    }
    """

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val ageGroup = validator.serviceDescription.get.models.head.fields.find { _.name == "age_group" }.get
    ageGroup.values should be(Seq("Twenties", "Thirties"))
  }

}
