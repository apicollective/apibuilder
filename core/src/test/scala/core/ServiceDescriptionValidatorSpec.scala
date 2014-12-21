package core

import lib.Primitives
import com.gilt.apidocspec.models.{Container, Datatype, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class ServiceValidatorSpec extends FunSpec with Matchers {

  it("should detect empty inputs") {
    val validator = ServiceValidator("")
    validator.isValid should be(false)
    validator.errors.mkString should be("No Data")
  }

  it("should detect invalid json") {
    val validator = ServiceValidator(" { ")
    validator.isValid should be(false)
    validator.errors.mkString.indexOf("expected close marker") should be >= 0
  }

  it("should detect all required fields") {
    val validator = ServiceValidator(" { } ")
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
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("Name[5@4] must start with a letter")
  }

  it("base url shouldn't end with a '/'") {
    val json = """
    {
      "name": "TestApp",
      "base_url": "http://localhost:9000/"
    }
    """

    val validator = ServiceValidator(json)
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
    val validator = ServiceValidator(json)
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
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("user.foo has invalid type. There is no model, enum, nor datatype named[foo]")
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
    val validator = ServiceValidator(json)
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
      "resources": [
        {
          "model": "user",
          "operations": [
            {
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val response = validator.serviceDescription.get.resources.head.operations.head.responses.head
    response.code should be(204)
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
      "resources": [
        {
          "model": "user",
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
      ]
    }
    """

    ServiceValidator(json.format("user")).isValid should be(true)
    ServiceValidator(json.format("unknown_model")).isValid should be(false)
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
      "resources": [
        {
          "model": "user",
          "operations": [
            {
              "method": "DELETE",
              "path": "/:guid"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Uuid.toString)))
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

    val validator = ServiceValidator(json)
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

    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    val idParam = op.parameters.head
    idParam.name should be("id")
    idParam.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Long.toString)))
  }

  it("lists of models are not valid in parameters") {

    val json = """
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
                        "method": "POST",
                        "parameters": [
                            { "name": "tags", "type": "[tags]" }
                        ]
                    }
                ]
            }
        }
    }
    """

    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("Resource[tag] POST /tags: Parameter[tags] has an invalid type[tags]")
  }

}
