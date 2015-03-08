package core

import org.scalatest.{FunSpec, Matchers}

class ServiceEnumSpec extends FunSpec with Matchers {

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
                { "name": "age_group", "type": "age_group", "required": false%s }
              ]
            }
          ]
        }
      }
    }
    """

  describe("defaults") {

    it("supports a known default") {
      val json = baseJson.format("", """, "default": "Twenties" """, "")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      val field = validator.service.models.head.fields.find(_.name == "age_group").get
      field.default should be(Some("Twenties"))
    }

    it("validates unknown defaults") {
      val json = baseJson.format("", """, "default": "other" """, "")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("user.age_group default[other] is not a valid value for enum[age_group]. Valid values are: Twenties, Thirties")
    }

    it("validates unknown defaults in parameters") {
      val json = baseJson.format("", "", """, "default": "other" """)
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("Resource[user] GET /users param[age_group] default[other] is not a valid value for enum[age_group]. Valid values are: Twenties, Thirties")
    }

  }

  it("field can be defined as an enum") {
    val json = baseJson.format("", "", "")
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val ageGroup = validator.service.models.head.fields.find { _.name == "age_group" }.get
    ageGroup.`type` should be("age_group")
  }

  it("validates that enum values do not start with numbers") {
    val json = baseJson.format(""", { "name": "1" } """, "", "")
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("Enum[age_group] value[1] is invalid: must start with a letter")
  }

}
