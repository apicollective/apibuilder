package core

import codegenerator.models.{Type, TypeKind}
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

  it("base url shouldn't end with a '/'") {
    val json = """
    {
      "name": "TestApp",
      "base_url": "http://localhost:9000/"
    }
    """

    val validator = ServiceDescriptionValidator(json)
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
    validator.errors.mkString should be("user.foo has invalid type. There is no model, enum, nor datatype named[foo]")
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
    guid.paramtype should be(Type(TypeKind.Primitive, Datatype.UuidType.name, false))
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

    val validator = ServiceDescriptionValidator(json)
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

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val op = validator.serviceDescription.get.resources.head.operations.head
    val idParam = op.parameters.head
    idParam.name should be("id")
    idParam.paramtype should be(Type(TypeKind.Primitive, "long", false))
  }

}
