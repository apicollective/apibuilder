package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionEnumSpec extends FunSpec with Matchers {

    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "enums": {
        "age_group": {
          "values": [
            { "name": "Twenties" },
            { "name": "Thirties" }%s
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "age_group"%s }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "parameters": [
                { "name": "age_group", "type": "age_group", "required": false }
              ]
            }
          ]
        }
      }
    }
    """

  describe("defaults") {

    it("supports a known default") {
      val json = baseJson.format("", """, "default": "Twenties" """)
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("")
      val field = validator.serviceDescription.get.models.head.fields.find(_.name == "age_group").get
      field.default should be(Some("Twenties"))
    }

    it("validates unknown defaults") {
      val json = baseJson.format("", """, "default": "other" """)
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("Model[user] field[age_group] Default[other] is not valid. Must be one of: Twenties, Thirties")
    }

  }

  it("field can be defined as an enum") {
    val json = baseJson.format("", "")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val ageGroup = validator.serviceDescription.get.models.head.fields.find { _.name == "age_group" }.get
    ageGroup.`type` should be(TypeInstance(TypeContainer.Singleton, Type.Enum("age_group")))
  }

  it("validates that enum values do not start with numbers") {
    val json = baseJson.format(""", { "name": "1" } """, "")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Enum[age_group] value[1] is invalid: must start with a letter")
  }

}
