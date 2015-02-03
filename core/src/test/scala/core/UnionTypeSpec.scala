package core

import org.scalatest.{FunSpec, Matchers}

// Placeholder for when we do union types
class UnionTypeSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "name": "Union Types Test",

      "unions": {
        "user": {
          "types": [
            { "type": "%s", "description": "foobar" },
            { "type": "%s" }
          ]
        }
      },

      "models": {

        "%s": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "guest": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "order": {
          "fields": [
            { "name": "id", "type": "uuid" },
            { "name": "user", "type": "user" }
          ]
        }

      }
    }
  """

  it("union types support descriptions") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("string", "uuid", "registered"))
    validator.errors.mkString("") should be("")
    val union = validator.service.get.unions.head
    union.types.find(_.`type` == "string").get.description should be(Some("foobar"))
    union.types.find(_.`type` == "uuid").get.description should be(None)
  }

  it("union types can have primitives") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("string", "uuid", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("string", "uuid"))
  }

  it("union types can have models") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", "registered", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("guest", "registered"))
  }

  it("union types can have lists") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("[guest]", "map[registered]", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("[guest]", "map[registered]"))
  }

  it("rejects blank types") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", "", "registered"))
    validator.errors.mkString("") should be("Union[user] all types must have a name")
  }

  it("rejects invalid types") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", "another_user", "registered"))
    validator.errors.mkString("") should be("Union[user] type[another_user] not found")
  }

  it("validates that union names do not overlap with model names") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("string", "uuid", "user"))
    validator.errors.mkString("") should be("Name[user] cannot be used as the name of both a model and a union type")
  }

}
