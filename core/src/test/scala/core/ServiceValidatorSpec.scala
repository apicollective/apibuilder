package core

import io.apibuilder.spec.v0.{models => spec}
import io.apibuilder.api.json.v0.models.ParameterLocation
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceValidatorSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

  it("should detect empty inputs") {
    val validator = TestHelper.serviceValidatorFromApiJson("")
    validator.errors().mkString should be("No Data")
  }

  it("should detect invalid json") {
    TestHelper.serviceValidatorFromApiJson(" { ").errors().mkString.indexOf("expected close marker") should be >= 0
  }

  it("service name must be a valid name") {
    TestHelper.serviceValidator(
      makeApiJson(name = "5@4")
    ).errors() should be(
      Seq("Name[5@4] must start with a letter")
    )
  }

  it("base url shouldn't end with a '/'") {
    TestHelper.serviceValidator(
      makeApiJson(baseUrl = Some("http://localhost:9000/"))
    ).errors() should be(
      Seq("base_url[http://localhost:9000/] must not end with a '/'")
    )
  }

  it("model that is missing fields") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModel(fields = Nil))
      )
    ).errors() should be(Nil)
  }

  it("model has a field with an invalid name") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "_!@#"))
        ))
      )
    ).errors() should be(
      Seq("Model[user] Field[_!@#] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters")
    )
  }

  it("model with duplicate field names") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(
            makeField(name = "key"),
            makeField(name = "KEY"),
          )
        ))
      )
    ).errors() should be(
      Seq("Model[user] field[key] appears more than once")
    )
  }

  it("reference that points to a non-existent model") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "id", `type` = "foo"))
        ))
      )
    ).errors() should be(
      Seq("Model[user] Field[id] type[foo] not found")
    )
  }

  it("types are lowercased in service definition") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "id", `type` = "UUID"))
        ))
      )
    ).service().models.head.fields.head.`type` should be("uuid")
  }

  it("base_url is optional") {
    TestHelper.serviceValidator(
      makeApiJson().copy(baseUrl = None)
    ).errors() should be(Nil)
  }

  it("defaults to a NoContent response") {
    TestHelper.serviceValidator(
      makeApiJson(
        models = Map("user" -> makeModelWithField()),
        resources = Map("user" -> makeResource(
          operations = Seq(
            makeOperation(method = "DELETE", path = Some("/:guid"))
          )
        ))
      )
    ).service().resources.head.operations.head.responses.find(r => TestHelper.responseCode(r.code) == "204").getOrElse {
      sys.error("Missing 204 response")
    }
  }

  it("accepts request header params") {
    def setup(typ: String) = {
      TestHelper.serviceValidator(
        makeApiJson(
          models = Map("user" -> makeModelWithField()),
          resources = Map("user" -> makeResource(
            operations = Seq(
              makeOperation(method = "DELETE", path = Some("/:guid"), parameters = Some(Seq(
                makeParameter(name = "guid", `type` = typ, location = ParameterLocation.Header)
              )))
            )
          ))
        )
      )
    }
    setup("string").errors() should be(Nil)
    val guid = setup("string").service().resources.head.operations.head.parameters.head
    guid.`type` should be("string")
    guid.location should be(spec.ParameterLocation.Header)

    setup("user").errors() should be(
      Seq("Resource[user] DELETE /users/:guid Parameter[guid] has an invalid type[user]. Interface, model and union types are not supported as header parameters.")
    )
  }

  it("accepts response headers") {
    val header = """{ "name": "foo", "type": "%s" }"""
    val json =
      """
    {
      "name": "API Builder",
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
              "responses": {
                "200": {
                  "type": "user",
                  "headers" : [
                    %s
                  ]
                }
              }
            }
          ]
        }
      }
    }
    """
    val stringHeader = header.format("string")
    val userHeader = header.format("user")

    val validator = TestHelper.serviceValidatorFromApiJson(json.format(stringHeader))
    validator.errors() should be(Nil)
    val headers = validator.service().resources.head.operations.head.responses.head.headers
    headers.size should be(1)
    headers.get.head.name should be("foo")
    headers.get.head.`type` should be("string")

    TestHelper.serviceValidatorFromApiJson(json.format(s"$stringHeader, $stringHeader")).errors().mkString("") should be("Resource[user] GET /users/:guid response code[200] header[foo] appears more than once")

    TestHelper.serviceValidatorFromApiJson(json.format(userHeader)).errors().mkString("") should be("Resource[user] GET /users/:guid response code[200] header[foo] type[user] is invalid: Must be a string or the name of an enum")
  }

  it("operations w/ a valid response validates correct") {
    val json =
      """
    {
      "name": "API Builder",
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

    TestHelper.serviceValidatorFromApiJson(json.format("user")).errors() should be(Nil)
    TestHelper.serviceValidatorFromApiJson(json.format("unknown_model")).errors() should be(
      Seq("Resource[user] GET /users/:guid response code[200] has an invalid type[unknown_model].")
    )
  }

  it("operations w/ a valid attributes validates correct") {
    val json =
      """
    {
      "name": "API Builder",
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
              "attributes": [
                {
                  "name": "sample",
                  "value": {}
                }
              ]
            }
          ]
        }
      }
    }
    """

    TestHelper.serviceValidatorFromApiJson(json).errors() should be(Nil)
  }

  it("includes path parameter in operations") {
    val json =
      """
    {
      "name": "API Builder",
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
    validator.errors() should be(Nil)
    val op = validator.service().resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.`type` should be("uuid")
  }

  it("DELETE supports query parameters") {
    val json =
      """
    {
      "name": "API Builder",
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
    validator.errors() should be(Nil)
    val op = validator.service().resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.`type` should be("[uuid]")
    guid.location should be(spec.ParameterLocation.Query)
  }

  it("path parameters must be required") {
    val json =
      """
    {
      "name": "API Builder",
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
    validator.errors().mkString("") should be("Resource[user] GET /users/:id path parameter[id] is specified as optional. All path parameters are required")
  }

  it("infers datatype for a path parameter from the associated model") {

    val json =
      """
    {
      "name": "API Builder",
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
    validator.errors() should be(Nil)
    val op = validator.service().resources.head.operations.head
    val idParam = op.parameters.head
    idParam.name should be("id")
    idParam.`type` should be("long")
  }

  describe("parameter validations") {

    val baseJson =
      """
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
      validator.errors() should be(Nil)
    }

    it("maps of primitives are valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("map[string]"))
      validator.errors().mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type[map[string]]. Maps are not supported as query parameters.")
    }

    it("lists of models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[tag]"))
      validator.errors() should be(
        Seq("Resource[tag] GET /tags Parameter[tags] has an invalid type[[tag]]. Valid nested types for lists in query parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid.")
      )
    }

    it("models are not valid in query parameters") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("tag"))
      validator.errors().mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type[tag]. Interface, model and union types are not supported as query parameters.")
    }

    it("validates type name in collection") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[foo]"))
      validator.errors().mkString("") should be("Resource[tag] GET /tags Parameter[tags] has an invalid type: [foo]")
    }

  }

  it("model with duplicate plural names are allowed as long as not exposed as resources") {
    val json =
      """
    {
      "name": "API Builder",
      "models": {
        "user": {
          "plural": "users",
          "fields": [
            { "name": "id", "type": "string" }
          ]
        },
        "person": {
          "plural": "users",
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET"
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors() should be(Nil)
  }

  it("resources with duplicate plural names are NOT allowed") {
        val json =
      """
    {
      "name": "API Builder",
      "models": {
        "user": {
          "plural": "users",
          "fields": [
            { "name": "id", "type": "string" }
          ]
        },
        "person": {
          "plural": "users",
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      },
      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET"
            }
          ]
        },
        "person": {
          "operations": [
            {
              "method": "GET"
            }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString("") should be("Resource with plural[users] appears more than once")
  }
}
