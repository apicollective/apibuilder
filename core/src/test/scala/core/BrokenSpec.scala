package core

import lib.{Datatype, Primitives, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class BrokenSpec extends FunSpec with Matchers {

  it("support arrays as types in fields") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "vendor": {
          "fields": [
            { "name": "guid", "type": "uuid" },
            { "name": "tags", "type": "[string]" }
          ]
        }
      }
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("")
    val fields = validator.serviceDescription.get.models.values.head.fields
    fields.find { _.name == "guid" }.get.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Uuid.toString)))
    fields.find { _.name == "tags" }.get.`type` should be(Datatype.List(Type(TypeKind.Primitive, Primitives.String.toString)))
  }


  it("support arrays as types in operations") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "vendor": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        }
      },
      "resources": [
        {
          "model": "vendor",
          "operations": [
            {
              "method": "POST",
              "parameters": [
                { "name": "guid", "type": "uuid" },
                { "name": "tag", "type": "[string]", "required": false }
              ],
              "responses": {
                "200": { "type": "vendor" }
              }
            }
          ]
        }
      ]
    }
    """
    val validator = ServiceValidator(json)
    validator.errors.mkString should be("")

    val operation = validator.serviceDescription.get.resources.values.head.operations.head
    operation.method should be("POST")
    operation.parameters.find { _.name == "guid" }.get.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Uuid.toString)))

    val guid = operation.parameters.find { _.name == "guid" }.get
    guid.`type` should be(Datatype.Singleton(Type(TypeKind.Primitive, Primitives.Uuid.toString)))
    guid.required should be(Some(true))

    val tag = operation.parameters.find { _.name == "tag" }.get
    tag.`type` should be(Datatype.List(Type(TypeKind.Primitive, Primitives.String.toString)))
    tag.required should be(Some(false))
  }


}
