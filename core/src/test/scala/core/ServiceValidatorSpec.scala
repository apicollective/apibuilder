package core

import io.apibuilder.spec.v0.{models => spec}
import io.apibuilder.api.json.v0.models.ParameterLocation
import io.apibuilder.api.json.v0.models.json._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ServiceValidatorSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

  it("should detect empty inputs") {
    TestHelper.expectSingleError("") should be("No Data")
  }

  it("should detect invalid json") {
    TestHelper.expectSingleError(" { ").indexOf("expected close marker") should be >= 0
  }

  it("service name must be a valid name") {
    TestHelper.expectSingleError(
      makeApiJson(name = "5@4")
    ) should be("Name[5@4] must start with a letter")
  }

  it("base url shouldn't end with a '/'") {
    TestHelper.expectSingleError(
      makeApiJson(baseUrl = Some("http://localhost:9000/"))
    ) should be("base_url[http://localhost:9000/] must not end with a '/'")
  }

  it("model that is missing fields") {
    setupValidApiJson(
      makeApiJson(
        models = Map("user" -> makeModel(fields = Nil))
      )
    )
  }

  it("model has a field with an invalid name") {
    TestHelper.expectSingleError(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "_!@#"))
        ))
      )
    ) should be(
      "Model[user] Field[_!@#] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters"
    )

  }

  it("model with duplicate field names") {
    TestHelper.expectSingleError(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(
            makeField(name = "key"),
            makeField(name = "KEY"),
          )
        ))
      )
    ) should be(
      "Model[user] field[key] appears more than once"
    )
  }

  it("reference that points to a non-existent model") {
    TestHelper.expectSingleError(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "id", `type` = "foo"))
        ))
      )
    ) should be(
      "Model[user] Field[id] type[foo] not found"
    )
  }

  it("types are lowercased in service definition") {
    setupValidApiJson(
      makeApiJson(
        models = Map("user" -> makeModel(
          fields = Seq(makeField(name = "id", `type` = "UUID"))
        ))
      )
    ).models.head.fields.head.`type` should be("uuid")
  }

  it("base_url is optional") {
    setupValidApiJson(
      makeApiJson().copy(baseUrl = None)
    )
  }

  it("defaults to a NoContent response") {
    setupValidApiJson(
      makeApiJson(
        models = Map("user" -> makeModelWithField()),
        resources = Map("user" -> makeResource(
          operations = Seq(
            makeOperation(method = "DELETE", path = Some("/:guid"))
          )
        ))
      )
    ).resources.head.operations.head.responses.find(_.code == "204").getOrElse {
      sys.error("Missing 204 response")
    }
  }

  it("accepts request header params") {
    def setup(typ: String) = {
      TestHelper.serviceValidatorFromApiJson(Json.prettyPrint(Json.toJson(
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
      )))
    }

    val guid = expectValid {
      setup("string")
    }.resources.head.operations.head.parameters.head
    guid.`type` should be("string")
    guid.location should be(spec.ParameterLocation.Path)

    expectInvalid {
      setup("user")  
    } should be(Seq(
      "Resource[user] DELETE /users/:guid path parameter[guid] has an invalid type[user]. Valid types for path parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
    ))
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

    
    val headers = setupValidApiJson(json.format(stringHeader)).resources.head.operations.head.responses.head.headers
    headers.size should be(1)
    headers.get.head.name should be("foo")
    headers.get.head.`type` should be("string")

    TestHelper.expectSingleError(json.format(s"$stringHeader, $stringHeader")) should be("Resource[user] GET /users/:guid response code[200] header[foo] appears more than once")

    TestHelper.expectSingleError(json.format(userHeader)) should be("Resource[user] GET /users/:guid response code[200] header[foo] type[user] is invalid: Must be a string or the name of an enum")
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

    setupValidApiJson(json.format("user"))
    TestHelper.expectSingleError(json.format("unknown_model")) should be(
      "Resource[user] GET /users/:guid response code[200] type[unknown_model] not found"
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

    setupValidApiJson(json)
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

    val op = setupValidApiJson(json).resources.head.operations.head
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

    val op = setupValidApiJson(json).resources.head.operations.head
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

    TestHelper.expectSingleError(json) should be("Resource[user] GET /users/:id path parameter[id] is specified as optional. All path parameters are required")
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

    val op = setupValidApiJson(json).resources.head.operations.head
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
      setupValidApiJson {
        baseJson.format("[string]")
      }
    }

    it("maps of primitives are valid in query parameters") {
      TestHelper.expectSingleError(baseJson.format("map[string]")) should be(
        "Resource[tag] GET /tags Parameter[tags] has an invalid type[map[string]]. Maps are not supported as query parameters."
      )
    }

    it("lists of models are not valid in query parameters") {
      TestHelper.expectSingleError(baseJson.format("[tag]")) should be(
        "Resource[tag] GET /tags Parameter[tags] has an invalid type[[tag]]. Valid nested types for lists in query parameters are: enum, boolean, decimal, integer, double, long, string, date-iso8601, date-time-iso8601, uuid."
      )
    }

    it("models are not valid in query parameters") {
      TestHelper.expectSingleError(baseJson.format("tag")) should be("Resource[tag] GET /tags Parameter[tags] has an invalid type[tag]. Interface, model and union types are not supported as query parameters.")
    }

    it("validates type name in collection") {
      TestHelper.expectSingleError(baseJson.format("[foo]")) should be("Resource[tag] GET /tags Parameter[tags] type[[foo]] not found")
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

    setupValidApiJson(json)
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

    TestHelper.expectSingleError(json) should be("Resource with plural[users] appears more than once")
  }
}
