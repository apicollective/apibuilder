package core

import com.bryzek.apidoc.spec.v0.models.ParameterLocation
import org.scalatest.{FunSpec, Matchers}

class ServiceValidatorSpec extends FunSpec with Matchers {

  it("should detect empty inputs") {
    val validator = TestHelper.serviceValidatorFromApiJson("")
    validator.errors.mkString should be("No Data")
  }

  it("should detect invalid json") {
    val validator = TestHelper.serviceValidatorFromApiJson(" { ")
    validator.errors.mkString.indexOf("expected close marker") should be >= 0
  }

  it("service name must be a valid name") {
    val json = """
    {
      "name": "5@4",
      "apidoc": { "version": "0.9.6" }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Name[5@4] must start with a letter")
  }

  it("base url shouldn't end with a '/'") {
    val json = """
    {
      "name": "TestApp",
      "base_url": "http://localhost:9000/",
      "apidoc": { "version": "0.9.6" }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("base_url[http://localhost:9000/] must not end with a '/'")
  }

  it("model that is missing fields") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": []
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Model[user] must have at least one field")
  }

  it("model has a field with an invalid name") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "_!@#", "type": "string" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Model[user] field name[_!@#] is invalid: Name can only contain a-z, A-Z, 0-9 and _ characters and Name must start with a letter")
  }

  it("model with duplicate field names") {
    val json = """
    {
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "key", "type": "string" },
            { "name": "KEY", "type": "string", "required": false }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("Model[user] field[key] appears more than once")
  }


  it("reference that points to a non-existent model") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "foo", "type": "foo" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("user.foo has invalid type[foo]")
  }

  it("types are lowercased in service definition") {
    val json = """
    {
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "UUID" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("")

    validator.service.models.head.fields.head.`type` should be("uuid")
  }

  it("base_url is optional") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("")
  }

  it("defaults to a NoContent response") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    validator.service.resources.head.operations.head.responses.find(r => TestHelper.responseCode(r.code) == "204").getOrElse {
      sys.error("Missing 204 response")
    }
  }

  it("operations w/ a valid response validates correct") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:guid",
              "parameters": [
                { "name": "guid", "type": "string" }
              ],
              "responses": {
                "200": { "type": "%s" }
              }
            }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors.mkString("") should be("")
    TestHelper.serviceValidatorFromApiJson(json.format("unknown_model")).errors.mkString("") should be("Resource[user] GET /users/:guid response code[200] has an invalid type[unknown_model].")
  }

  it("includes path parameter in operations") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val op = validator.service.resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.`type` should be("uuid")
  }

  it("DELETE supports query parameters") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "DELETE",
              "parameters": [
                  { "name": "guid", "type": "[uuid]" }
              ]
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val op = validator.service.resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.`type` should be("[uuid]")
    guid.location should be(ParameterLocation.Query)
  }

  it("path parameters must be required") {
    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },
      "resources": {
       "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id",
              "parameters": [
                { "name": "id", "type": "long", "required": false }
              ]
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("Resource[user] GET /users/:id path parameter[id] is specified as optional. All path parameters are required")
  }

  it("infers datatype for a path parameter from the associated model") {

    val json = """
    {
      "name": "Api Doc",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },
      "resources": {
       "user": {
          "operations": [
            {
              "method": "DELETE",
              "path": "/:id"
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString("") should be("")
    val op = validator.service.resources.head.operations.head
    val idParam = op.parameters.head
    idParam.name should be("id")
    idParam.`type` should be("long")
  }

  describe("parameter validations") {

    val baseJson = """
    {
        "name": "Test Validation of Parameters",
        "apidoc": { "version": "0.9.6" },
    
        "models": {
    
            "tag": {
                "fields": [
                    { "name": "name", "type": "string" }
                ]
            }
    
        },

        "resources": {
          "tag": {
                "operations": [
                    {
                        "method": "GET",
                        "parameters": [
                            { "name": "tags", "type": "%s" }
                        ]
                    }
                ]
          }
        }
    }
    """

    it("lists of primitives are valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[string]"))
      validator.errors should be(Nil)
    }

    it("maps of primitives are valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("map[string]"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type[map[string]]. Maps are not supported as query parameters.")
    }

    it("lists of models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[tag]"))
      validator.errors should be(
        Seq("Resource[tag] GET /tags Parameter[tags] has an invalid type[[tag]]. Parameters that are lists must be lists of primitive types or enums.")
      )
    }

    it("models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("tag"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type[tag]. Model and union types are not supported as query parameters.")
    }

    it("validates type name in collection") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[foo]"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type: [foo]")
    }

  }

}
