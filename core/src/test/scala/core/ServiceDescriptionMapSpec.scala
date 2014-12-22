package core

import lib.{Datatype, Primitives, Type, TypeKind}
import org.scalatest.{FunSpec, Matchers}

class ServiceMapSpec extends FunSpec with Matchers {

  private val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            %s
          ]
        }
      }
    }
  """

  it("accepts type: map, defaulting to element type of string for backwards compatibility") {
    val json = baseJson.format("""{ "name": "tags", "type": "map" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.values.head.fields.head
    tags.`type` should be(Datatype.Map(Type(TypeKind.Primitive, Primitives.String.toString)))
  }

  it("accept defaults for maps") {
    val json = baseJson.format("""{ "name": "tags", "type": "map", "default": "{ }" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.values.head.fields.head
    tags.default shouldBe Some("{ }")
  }

  it("validates invalid json") {
    val json = baseJson.format("""{ "name": "tags", "type": "map", "default": "bar" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("default[bar] is not valid json")
  }

  it("accepts valid defaults for map[string]") {
    val json = baseJson.format("""{ "name": "tags", "type": "map[string]", "default": "{ \"foo\": \"bar\" }" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
  }

  it("accepts valid defaults for map[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "map[integer]", "default": "{ \"foo\": 1 }" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
  }

  it("rejects invalid json objects for map[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "map[integer]", "default": "1" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("default[1] is not a valid JSON Object")
  }

  it("rejects invalid defaults for map[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "map[integer]", "default": "{ \"foo\": \"bar\" }" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("user.tags Value[bar] is not a valid integer")
  }

  it("accepts valid defaults for list[string]") {
    val json = baseJson.format("""{ "name": "tags", "type": "[string]", "default": "[\"foo\", \"bar\"]" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
  }

  it("accepts valid defaults for list[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "[integer]", "default": "[1]" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("")
  }

  it("rejects invalid json objects for list[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "[integer]", "default": "1" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("default[1] is not a valid JSON Array")
  }

  it("rejects invalid defaults for list[integer]") {
    val json = baseJson.format("""{ "name": "tags", "type": "[integer]", "default": "[\"bar\"]" }""")
    val validator = ServiceValidator(json)
    validator.errors.mkString("") should be("user.tags Value[bar] is not a valid integer")
  }

}
