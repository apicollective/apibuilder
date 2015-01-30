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

        "registered_user": {
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
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("string", "uuid"))
    validator.errors.mkString("") should be("")
    val union = validator.service.get.unions.head
    union.types.find(_.`type` == "string").get.description should be(Some("foobar"))
    union.types.find(_.`type` == "uuid").get.description should be(None)
  }

  it("union types can have primitives") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("string", "uuid"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("string", "uuid"))
  }

  it("union types can have models") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", "registered_user"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("guest", "registered_user"))
  }

  it("union types can have lists") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("[guest]", "map[registered_user]"))
    validator.errors.mkString("") should be("")
    validator.service.get.unions.head.types.map(_.`type`) should be(Seq("[guest]", "map[registered_user]"))
  }

  it("rejects blank types") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", ""))
    validator.errors.mkString("") should be("Union[user] all types must have a name")
  }

  it("rejects invalid types") {
    val validator = ServiceValidator(TestHelper.serviceConfig, baseJson.format("guest", "another_user"))
    validator.errors.mkString("") should be("Union[user] type[another_user] not found")
  }

}
