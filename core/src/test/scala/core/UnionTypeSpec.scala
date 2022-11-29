package core

import cats.data.Validated.{Invalid, Valid}
import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

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
    def setupTypes(js: String) = {
      setupValidApiJson(js).unions.head.types

    }

    it("union types support descriptions") {
      val union = setupValidApiJson(baseJson.format("", "string", "uuid", "registered")).unions.head
      union.types.find(_.`type` == "string").get.description should be(Some("foobar"))
      union.types.find(_.`type` == "uuid").get.description should be(None)
    }

    it("union types can have primitives") {
      setupTypes(baseJson.format("", "string", "uuid", "registered")).map(_.`type`) should be(Seq("string", "uuid"))
    }

    it("union types can have models") {
      setupTypes(baseJson.format("", "guest", "registered", "registered")).map(_.`type`) should be(Seq("guest", "registered"))
    }

    it("union types can have lists") {
      setupValidApiJson(baseJson.format("", "[guest]", "map[registered]", "registered")).unions.head.types.map(_.`type`) should be(Seq("[guest]", "map[registered]"))
    }

    it("rejects blank types") {
      TestHelper.expectSingleError(baseJson.format("", "guest", "", "registered")) should be(
        "Union[user] type[] type must be a non empty string"
      )
    }

    it("rejects circular type") {
      TestHelper.expectSingleError(baseJson.format("", "user", "guest", "registered")) should be(
        "Union[user] cannot contain itself as one of its types or sub-types"
      )
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
      TestHelper.expectSingleError(json) should be("Union[bar] cannot contain itself as one of its types or sub-types: bar->baz->foo->bar")
    }

    it("rejects invalid types") {
      TestHelper.expectSingleError(baseJson.format("", "guest", "another_user", "registered")) should be(
        "Union[user] type[another_user] not found"
      )
    }

    it("validates that union names do not overlap with model names") {
      TestHelper.expectSingleError(baseJson.format("", "string", "uuid", "user")) should be(
        "Name[user] cannot be used as the name of both a model and a union type"
      )
    }

    it("validates unit type") {
      TestHelper.expectSingleError(
        makeApiJson(
          unions = Map(
            "user" -> makeUnion(
              types = Seq(makeUnionType(`type` = "unit"))
            )
          )
        )
      ) should be(
        "Union[user] Union types cannot contain unit. To make a particular field optional, use the required property."
      )
    }

    it("infers proper parameter type if field is common across all types") {
      setupValidApiJson(baseJson.format("", "guest", "registered", "registered")).resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
        sys.error("Could not find guid parameter")
      }.`type` should be("uuid")
    }

    it("infers string parameter type if type varies") {
      setupValidApiJson(baseJson.format("", "other_random_user", "registered", "registered")).resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
        sys.error("Could not find guid parameter")
      }.`type` should be("string")
    }

  }

  describe("with discriminator") {

    it("union types unique discriminator") {
      setupValidApiJson(baseJson.format(""""discriminator": "type",""", "guest", "registered", "registered")).unions.head.discriminator should be(Some("type"))
    }

    it("validates union types discriminator that is not a defined field") {
      TestHelper.expectSingleError(baseJson.format(""""discriminator": "id",""", "guest", "registered", "registered")) should be(
        "Union[user] discriminator[id] must be unique. Field exists on: guest, registered"
      )
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
      setupValidApiJson(nestedUnionTypeJson("type", "type"))
    }

    it("invalid discriminator") {
      expectInvalid {
        TestHelper.serviceValidatorFromApiJson(nestedUnionTypeJson("foo", "foo"))
      } should be(Seq(
        "Union[guest] discriminator[foo] must be unique. Field exists on: anonymous",
        "Union[user] discriminator[foo] must be unique. Field exists on: guest.anonymous"
      ))
    }

    it("non text discriminator") {
      TestHelper.expectSingleError(nestedUnionTypeJson("!@KL#", "type")) should be(
        "Union[user] discriminator[!@KL#] is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter"
      )
    }

    it("'value' is reserved for primitive wrappers") {
      TestHelper.expectSingleError(nestedUnionTypeJson("value", "type")) should be(
        "Union[user] discriminator[value]: The keyword[value] is reserved and cannot be used as a discriminator"
      )
    }

    it("'implicit' is reserved for future implicit discriminators") {
      TestHelper.expectSingleError(nestedUnionTypeJson("implicit", "type")) should be(
        "Union[user] discriminator[implicit]: The keyword[implicit] is reserved and cannot be used as a discriminator"
      )
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


    setupValidApiJson(json).resources.head.operations.head.parameters.find(_.name == "id").getOrElse {
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

    val service = setupValidApiJson(common)
    service.namespace should be("test.common")
    service.models.map(_.name) should be(Seq("reference"))

    val fetcher = MockServiceFetcher()
    fetcher.add(uri, service)

    expectInvalid {
      TestHelper.serviceValidatorFromApiJson(user, fetcher = fetcher)
    } should be(
      Seq("Union[expandable_user] type[test.common.models.reference] is invalid. Cannot use an imported type as part of a union as there is no way to declare that the imported type expands the union type defined here.")
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
      ) match {
        case Invalid(e) => e.toNonEmptyList.toList
        case Valid(_) => Nil
      }
    }
    test(userDefault = false, guestDefault = false) should be(Nil)
    test(userDefault = true, guestDefault = false) should be(Nil)
    test(userDefault = false, guestDefault = true) should be(Nil)
    test(userDefault = true, guestDefault = true) should be(
      Seq("Union[visitor] Only 1 type can be specified as default. Currently the following types are marked as default: guest, user")
    )
  }

}
