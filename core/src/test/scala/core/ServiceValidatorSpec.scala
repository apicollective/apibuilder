package core

import com.gilt.apidoc.spec.v0.models.ParameterLocation
import org.scalatest.{FunSpec, Matchers}

class ServiceValidatorSpec extends FunSpec with Matchers {

  it("should detect empty inputs") {
    val validator = TestHelper.serviceValidatorFromApiJson("")
    validator.isValid should be(false)
    validator.errors.mkString should be("No Data")
  }

  it("should detect invalid json") {
    val validator = TestHelper.serviceValidatorFromApiJson(" { ")
    validator.isValid should be(false)
    validator.errors.mkString.indexOf("expected close marker") should be >= 0
  }

  it("should detect all required fields") {
    val validator = TestHelper.serviceValidatorFromApiJson(" { } ")
    validator.isValid should be(false)
    validator.errors.mkString should be("Missing: name")
  }

  it("service name must be a valid name") {
    val json = """
    {
      "name": "5@4",
      "base_url": "http://localhost:9000"
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Name[5@4] must start with a letter")
  }

  it("base url shouldn't end with a '/'") {
    val json = """
    {
      "name": "TestApp",
      "base_url": "http://localhost:9000/"
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("base_url[http://localhost:9000/] must not end with a '/'")
  }

  it("model that is missing fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors.mkString should be("Model[user] must have at least one field")
    validator.isValid should be(false)
  }

  it("reference that points to a non-existent model") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
    validator.isValid should be(false)
  }

  it("base_url is optional") {
    val json = """
    {
      "name": "Api Doc",
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
    validator.isValid should be(true)
  }

  it("defaults to a NoContent response") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
    validator.service.resources.head.operations.head.responses.find(_.code == 204).getOrElse {
      sys.error("Missing 204 response")
    }
  }

  it("operations w/ a valid response validates correct") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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

    TestHelper.serviceValidatorFromApiJson(json.format("user")).isValid should be(true)
    TestHelper.serviceValidatorFromApiJson(json.format("unknown_model")).isValid should be(false)
  }

  it("includes path parameter in operations") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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

  it("path parameters cannot be optional") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
    validator.errors.mkString("") should be("Resource[user] GET path parameter[id] is specified as optional. All path parameters are required")
  }

  it("infers datatype for a path parameter from the associated model") {

    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
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
        "base_url": "https://localhost",
        "name": "Test Validation of Parameters",
    
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
      validator.errors.mkString("") should be("")
    }

    it("maps of primitives are valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("map[string]"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags: Parameter[tags] has an invalid type[map[string]]. Maps are not supported as query parameters.")
    }

    it("lists of models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[tag]"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags: Parameter[tags] has an invalid type[tag]. Model and union types are not supported as query parameters.")
    }

    it("models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("tag"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags: Parameter[tags] has an invalid type[tag]. Model and union types are not supported as query parameters.")
    }

    it("validates type name in collection") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[foo]"))
      validator.errors.mkString("") should be("Resource[tag] GET /tags: Parameter[tags] has an invalid type: [foo]")
    }

  }

}
