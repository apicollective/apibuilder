package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

    private[this] val baseJson = """
    {
      "name": "Union Types Test",

      "unions": {
        "user": { %s
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

        "other_random_user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        },

        "order": {
          "fields": [
            { "name": "id", "type": "uuid" },
            { "name": "user", "type": "user" }
          ]
        }

      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id"
            }
          ]
        }
      }
    }
  """

  describe("without discriminator") {

    it("union types support descriptions") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "string", "uuid", "registered"))
      validator.errors() should be(Nil)
      val union = validator.service().unions.head
      union.types.find(_.`type` == "string").get.description should be(Some("foobar"))
      union.types.find(_.`type` == "uuid").get.description should be(None)
    }

    it("union types can have primitives") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "string", "uuid", "registered"))
      validator.errors() should be(Nil)
      validator.service().unions.head.types.map(_.`type`) should be(Seq("string", "uuid"))
    }

    it("union types can have models") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "guest", "registered", "registered"))
      validator.errors() should be(Nil)
      validator.service().unions.head.types.map(_.`type`) should be(Seq("guest", "registered"))
    }

    it("union types can have lists") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "[guest]", "map[registered]", "registered"))
      validator.errors() should be(Nil)
      validator.service().unions.head.types.map(_.`type`) should be(Seq("[guest]", "map[registered]"))
    }

    it("rejects blank types") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "guest", "", "registered"))
      validator.errors() should be(Seq("Union[user] type[] type must be a non empty string"))
    }

    it("rejects circular type") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "user", "guest", "registered"))
      validator.errors() should be(Seq("Union[user] cannot contain itself as one of its types or sub-types"))
    }

    it("rejects indirectly circular type") {
      val json = """{
        "name": "Union Types Test",

        "unions": {
          "foo": {
            "types": [ { "type": "bar" } ]
          },
          "bar": {
            "types": [ { "type": "baz" } ]
          },
          "baz": {
            "types": [ { "type": "foo" } ]
          }
        },
        "models": {},
        "resources": {}
      }"""
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors() should be(Seq("Union[bar] cannot contain itself as one of its types or sub-types: bar->baz->foo->bar"))
    }

    it("rejects invalid types") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "guest", "another_user", "registered"))
      validator.errors() should be(Seq("Union[user] type[another_user] not found"))
    }

    it("validates that union names do not overlap with model names") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "string", "uuid", "user"))
      validator.errors() should be(Seq("Name[user] cannot be used as the name of both a model and a union type"))
    }

    it("validates unit type") {
      TestHelper.serviceValidator(
        makeApiJson(
          unions = Map(
            "user" -> makeUnion(
              types = Seq(makeUnionType(`type` = "unit"))
            )
          )
        )
      ).errors() should equal(
        Seq("Union[user] Union types cannot contain unit. To make a particular field optional, use the required property.")
      )
    }

    it("infers proper parameter type if field is common across all types") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "guest", "registered", "registered"))
      validator.service().resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
        sys.error("Could not find guid parameter")
      }.`type` should be("uuid")
    }

    it("infers string parameter type if type varies") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format("", "other_random_user", "registered", "registered"))
      validator.service().resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
        sys.error("Could not find guid parameter")
      }.`type` should be("string")
    }

  }

  describe("with discriminator") {

    it("union types unique discriminator") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format(""""discriminator": "type",""", "guest", "registered", "registered"))
      validator.errors() should be(Nil)
      val union = validator.service().unions.head
      union.discriminator should be(Some("type"))
    }

    it("validates union types discriminator that is not a defined field") {
      val validator = TestHelper.serviceValidatorFromApiJson(baseJson.format(""""discriminator": "id",""", "guest", "registered", "registered"))
      validator.errors() should be(Seq("Union[user] discriminator[id] must be unique. Field exists on: guest, registered"))
    }

  }

  describe("with nested union type") {
    def nestedUnionTypeJson(userDiscriminator: String, guestDiscriminator: String): String = s"""
    {
      "name": "Union Types Test",

      "unions": {
        "user": {
          "discriminator": "$userDiscriminator",
          "types": [
            { "type": "registered" },
            { "type": "guest" }
          ]
        },

        "guest": {
          "discriminator": "$guestDiscriminator",
          "types": [
            { "type": "uuid" },
            { "type": "anonymous" }
          ]
        }
      },

      "models": {

        "registered": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "anonymous": {
          "fields": [
            { "name": "id", "type": "uuid" },
            { "name": "foo", "type": "string" }
          ]
        }
      }
    }
  """

    it("valid discriminator") {
      val validator = TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("type", "type"))
      validator.errors() should be(Nil)
    }

    it("invalid discriminator") {
      val validator = TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("foo", "foo"))
      validator.errors() should be(Seq(
        "Union[guest] discriminator[foo] must be unique. Field exists on: anonymous",
        "Union[user] discriminator[foo] must be unique. Field exists on: guest.anonymous"
      ))
    }

    it("non text discriminator") {
      val validator = TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("!@KL#", "type"))
      validator.errors() should be(Seq(
        "Union[user] discriminator[!@KL#]: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter"
      ))
    }

    it("'value' is reserved for primitive wrappers") {
      val validator = TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("value", "type"))
      validator.errors() should be(Seq(
        "Union[user] discriminator[value]: The keyword[value] is reserved and cannot be used as a discriminator"
      ))
    }

    it("'implicit' is reserved for future implicit discriminators") {
      val validator = TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("implicit", "type"))
      validator.errors() should be(Seq(
        "Union[user] discriminator[implicit]: The keyword[implicit] is reserved and cannot be used as a discriminator"
      ))
    }

  }

  it("infers proper parameter type if field is common across all types including primitive") {
    val json = """
    {
      "name": "Union Types Test",

      "unions": {
        "user": {
          "types": [
            { "type": "registered" },
            { "type": "uuid" }
          ]
        }
      },

      "models": {

        "registered": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }

      },

      "resources": {
        "user": {
          "operations": [
            {
              "method": "GET",
              "path": "/:id"
            }
          ]
        }
      }
    }
  """


    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.service().resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
      sys.error("Could not find guid parameter")
    }.`type` should be("uuid")
  }

  it("does not allow a type from an imported service") {
    // This doesn't work because there is no way the imported class
    // can extend the union trait that is defined in this service.

    val common = """
    {
      "name": "common",
      "namespace": "test.common",
      "models": {
        "reference": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      }
    }
    """

    val uri = "http://localhost/test/common/0.0.1/service.json"
    val user = s"""
    {
      "name": "user",
      "imports": [ { "uri": "$uri" } ],

      "unions": {
        "expandable_user": {
          "types": [
            { "type": "test.common.models.reference" },
            { "type": "user" }
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(common)
    validator.errors() should be(Nil)
    validator.service().namespace should be("test.common")
    validator.service().models.map(_.name) should be(Seq("reference"))

    val fetcher = MockServiceFetcher()
    fetcher.add(uri, validator.service())

    TestHelper.serviceValidatorFromApiJson(user, fetcher = fetcher).errors() should be(
      Seq("Union[expandable_user] Type[test.common.models.reference] is invalid. Cannot use an imported type as part of a union as there is no way to declare that the imported type expands the union type defined here.")
    )

  }

  it("only one type can be marked default") {
    def test(userDefault: Boolean = false, guestDefault: Boolean = false) = {
      TestHelper.serviceValidator(
        makeApiJson(
          models = Map("user" -> makeModelWithField(), "guest" -> makeModelWithField()),
          unions = Map("visitor" -> makeUnion(
            discriminator = Some("discriminator"),
            types = Seq(
              makeUnionType(`type` = "user", default = userDefault),
              makeUnionType(`type` = "guest", default = guestDefault),
            )
          ))
        )
      )
    }
    test(userDefault = false, guestDefault = false).errors() should be(Nil)
    test(userDefault = true, guestDefault = false).errors() should be(Nil)
    test(userDefault = false, guestDefault = true).errors() should be(Nil)
    test(userDefault = true, guestDefault = true).errors() should be(
      Seq("Union[visitor] Only 1 type can be specified as default. Currently the following types are marked as default: guest, user")
    )
  }

}
