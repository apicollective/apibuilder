package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServicePathParametersSpec extends FunSpec with Matchers {

  describe("with a service") {
    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

      "enums": {
        "age_group": {
          "values": [
            { "name": "Youth" },
            { "name": "Adult" }
          ]
        }
      },

      "models": {

        "tag": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "user": {
          "fields": [
            { "name": "id", "type": "long" },
            { "name": "name", "type": "string" },
            { "name": "created_at_date", "type": "date-iso8601" },
            { "name": "created_at_date_time", "type": "date-time-iso8601" },
            { "name": "tag", "type": "tag" },
            { "name": "tags", "type": "map" },
            { "name": "age_group", "type": "age_group" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "%s",
              "path": "%s"
            }
          ]
        }
      }
    }
    """

    it("numbers can be path parameters") {
      val json = baseJson.format("GET", "/:id")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("strings can be path parameters") {
      val json = baseJson.format("GET", "/:name")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("parameters not defined on the model are accepted (assumed strings)") {
      val json = baseJson.format("GET", "/:some_string")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("enums can be path parameters - assumed type is string") {
      val json = baseJson.format("GET", "/:age_group")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("dates can be path parameters") {
      val json = baseJson.format("GET", "/:created_at_date")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("date-time can be path parameters") {
      val json = baseJson.format("GET", "/:created_at_date_time")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("")
    }

    it("other models cannot be path parameters") {
      val json = baseJson.format("GET", "/:tag")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("Resource[user] GET path parameter[tag] has an invalid type[tag]. Valid types for path parameters are: boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid")
    }

    it("unsupported types declared as parameters are validated") {
      val json = baseJson.format("POST", "/:tags")
      TestHelper.serviceValidatorFromApiJson(json).errors.mkString("") should be("Resource[user] POST path parameter[tags] has an invalid type[map]. Valid types for path parameters are: boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid")
    }

  }

  describe("w/ a union resource") {

    val baseJson = s"""
    {
      "name": "Api Doc",
      "unions": {
        "user": {
          "types": [
            { "type": "registered" },
            { "type": "guest" }
          ]
        }
      },

      "models": {
        "registered": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "age", "type": "integer" }
          ]
        },
        "guest": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "age", "type": "long" }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            { "method": "GET", "path": "/users/:%s" }
          ]
        }
      }
    }
    """

    it("can identify common type for path parameter if all union types have the same type") {
      val json = baseJson.format("guid")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      val userResource = validator.service.resources.head
      val op = userResource.operations.head
      val param = op.parameters.head
      param.name should be("guid")
      param.`type` should be("uuid")
    }

    it("uses default 'string' if path parameter type varies across union type") {
      val json = baseJson.format("age")
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors.mkString("") should be("")
      val userResource = validator.service.resources.head
      val op = userResource.operations.head
      val param = op.parameters.head
      param.name should be("age")
      param.`type` should be("string")
    }

  }

}
