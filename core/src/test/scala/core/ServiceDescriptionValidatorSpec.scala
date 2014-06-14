package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionValidatorSpec extends FunSpec with Matchers {

  it("should detect empty inputs") {
    val validator = ServiceDescriptionValidator("")
    validator.isValid should be(false)
    validator.errors.mkString should be("No Data")
  }

  it("should detect invalid json") {
    val validator = ServiceDescriptionValidator(" { ")
    validator.isValid should be(false)
    validator.errors.mkString.indexOf("expected close marker") should be >= 0
  }

  it("should detect all required fields") {
    val validator = ServiceDescriptionValidator(" { } ")
    validator.isValid should be(false)
    validator.errors.mkString should be("Missing: base_url, name")
  }

  it("service name must be a valid name") {
    val json = """
    {
      "name": "5@4",
      "base_url": "http://localhost:9000"
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("Name[5@4] must start with a letter")
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
    val validator = ServiceDescriptionValidator(json)
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
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString should be("user.foo has invalid type. There is no model nor datatype named[foo]")
    validator.isValid should be(false)
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
    val validator = ServiceDescriptionValidator(json)
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

    ServiceDescriptionValidator(json.format("user")).isValid should be(true)
    ServiceDescriptionValidator(json.format("unknown_model")).isValid should be(false)
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
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    op.parameters.map(_.name) should be(Seq("guid"))
    val guid = op.parameters.head
    guid.paramtype should be(PrimitiveParameterType(Datatype.StringType)) // TODO: Should we look up the field and infer UUID type?
  }

  it("fails for GET operations with bodies") {
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
              "method": "GET",
              "path": "/:guid",
              "body": "user"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Operation[user#GET /users/:guid: cannot define a body with method GET")
  }

  it("fails for DELETE operations with bodies") {
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
              "path": "/:guid",
              "body": "user"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Operation[user#DELETE /users/:guid: cannot define a body with method DELETE")
  }

  it("succeeds for PATCH operations without bodies") {
    ServiceDescriptionValidator(s"""
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
              "method": "PATCH",
              "path": "/:guid"
            }
          ]
        }
      ]
    }
    """).errors.mkString("") should be("")
  }

  it("fails for PATCH operations with bodies") {
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
              "method": "PATCH",
              "path": "/:guid",
              "body": "user"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Operation[user#PATCH /users/:guid: cannot define a body with method PATCH. Only the default is allowed.")
  }

  it("succeeds for POST operations with legal bodies") {
    Seq("user", "unit", "file").foreach { bodyType =>
      ServiceDescriptionValidator(s"""
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
                "method": "POST",
                "path": "/:guid",
                "body": "$bodyType"
              }
            ]
          }
        ]
      }
      """).errors.mkString("") should be("")
    }
  }

  it("fails for POST operations with illegal bodies") {
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
              "method": "POST",
              "path": "/:guid",
              "body": "foo"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Operation[user#POST /users/:guid: has illegal body type: foo")
  }

  it("succeeds for PUT operations with legal bodies") {
    Seq("user", "unit", "file").foreach { bodyType =>
      ServiceDescriptionValidator(s"""
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
                "method": "PUT",
                "path": "/:guid",
                "body": "$bodyType"
              }
            ]
          }
        ]
      }
      """).errors.mkString("") should be("")
    }
  }

  it("fails for PUT operations with illegal bodies") {
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
              "method": "PUT",
              "path": "/:guid",
              "body": "bar"
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Operation[user#PUT /users/:guid: has illegal body type: bar")
  }

}
