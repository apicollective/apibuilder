package core

import org.scalatest.{FunSpec, Matchers}

class DeprecationSpec extends FunSpec with Matchers {

  it("enum") {
    val json = """
    {
      "name": "Api Doc",

      "enums": {
        "old_content_type": {
          "deprecation": { "description": "blah" },
          "values": [
            { "name": "application/json" },
            { "name": "application/xml" }
          ]
        },

        "content_type": {
          "values": [
            { "name": "application/json" },
            { "name": "application/xml" }
          ]
        }
      }
    }
    """

    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
    validator.service.get.enums.find(_.name == "old_content_type").get.deprecation.flatMap(_.description) should be(Some("blah"))
    validator.service.get.enums.find(_.name == "content_type").get.deprecation.flatMap(_.description) should be(None)
  }

  it("enum value") {
    val json = """
    {
      "name": "Api Doc",

      "enums": {
        "content_type": {
          "values": [
            { "name": "application/json", "deprecation": { "description": "blah" } },
            { "name": "application/xml" }
          ]
        }
      }
    }
    """

    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
    val ct = validator.service.get.enums.find(_.name == "content_type").get

    ct.values.find(_.name == "application/json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    ct.values.find(_.name == "application/xml").get.deprecation.flatMap(_.description) should be(None)
  }

  it("union") {
    val json = """
    {
      "name": "Api Doc",

      "unions": {
        "old_content_type": {
          "deprecation": { "description": "blah" },
          "types": [
            { "type": "api_json" },
            { "type": "avro_idl" }
          ]
        },

        "content_type": {
          "types": [
            { "type": "api_json" },
            { "type": "avro_idl" }
          ]
        }
      },

      "models": {

        "api_json": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "avro_idl": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }

      }
    }
    """

    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
    validator.service.get.unions.find(_.name == "old_content_type").get.deprecation.flatMap(_.description) should be(Some("blah"))
    validator.service.get.unions.find(_.name == "content_type").get.deprecation.flatMap(_.description) should be(None)
  }

  it("union type") {
    val json = """
    {
      "name": "Api Doc",

      "unions": {
        "content_type": {
          "types": [
            { "type": "api_json", "deprecation": { "description": "blah" } },
            { "type": "avro_idl" }
          ]
        }
      },

      "models": {

        "api_json": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "avro_idl": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }

      }
    }
    """

    val validator = ServiceValidator(TestHelper.serviceConfig, json)
    validator.errors.mkString("") should be("")
    val union = validator.service.get.unions.find(_.name == "content_type").get
    union.types.find(_.`type` == "api_json").get.deprecation.flatMap(_.description) should be(Some("blah"))
    union.types.find(_.`type` == "avro_idl").get.deprecation.flatMap(_.description) should be(None)
  }

}
