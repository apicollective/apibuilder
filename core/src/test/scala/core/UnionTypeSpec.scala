package core

import org.scalatest.{FunSpec, Matchers}

// Placeholder for when we do union types
class UnionTypeSpec extends FunSpec with Matchers {

  val baseJson = """
    {
      "name": "Union Types Test",
      "apidoc": { "version": "0.9.6" },

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
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("string", "uuid", "registered"))
    validator.errors.mkString("") should be("")
    val union = validator.service.unions.head
    union.types.find(_.`type` == "string").get.description should be(Some("foobar"))
    union.types.find(_.`type` == "uuid").get.description should be(None)
  }

  it("union types can have primitives") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("string", "uuid", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.unions.head.types.map(_.`type`) should be(Seq("string", "uuid"))
  }

  it("union types can have models") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("guest", "registered", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.unions.head.types.map(_.`type`) should be(Seq("guest", "registered"))
  }

  it("union types can have lists") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("[guest]", "map[registered]", "registered"))
    validator.errors.mkString("") should be("")
    validator.service.unions.head.types.map(_.`type`) should be(Seq("[guest]", "map[registered]"))
  }

  it("rejects blank types") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("guest", "", "registered"))
    validator.errors.mkString("") should be("Union[user] type[] type must be a non empty string")
  }

  it("rejects invalid types") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("guest", "another_user", "registered"))
    validator.errors.mkString("") should be("Union[user] type[another_user] not found")
  }

  it("validates that union names do not overlap with model names") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("string", "uuid", "user"))
    validator.errors.mkString("") should be("Name[user] cannot be used as the name of both a model and a union type")
  }

  it("validates unit type") {
    val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("unit", "uuid", "registered"))
    validator.errors.mkString("") should be("Union types cannot contain unit. To make a particular field optional, use the required property.")
  }

}
