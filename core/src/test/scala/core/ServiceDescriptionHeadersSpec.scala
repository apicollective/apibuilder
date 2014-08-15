package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionHeadersSpec extends FunSpec with Matchers {

  describe("valid service") {
    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

      "headers": [
        { "name": "Content-Type", "type": "content_type" },
        { "name": "X-Foo", "type": "string", "value": "bar", "description": "test" }
      ],

      "enums": {
        "content_type": {
          "values": [
            { "name": "application/json" },
            { "name": "application/xml" }
          ]
        }
      }
    }
  """

    it("parses headers") {
      val validator = ServiceDescriptionValidator(baseJson)
      validator.errors.mkString("") should be("")
      val ctEnum = validator.serviceDescription.get.enums.find(_.name == "content_type").get

      val ct = validator.serviceDescription.get.headers.find(_.name == "Content-Type").get
      ct.name should be("Content-Type")
      ct.headertype should be(EnumHeaderType(ctEnum))
      ct.value should be(None)
      ct.description should be(None)

      val foo = validator.serviceDescription.get.headers.find(_.name == "X-Foo").get
      foo.name should be("X-Foo")
      foo.headertype should be(StringHeaderType)
      foo.value should be(Some("bar"))
      foo.description should be(Some("test"))
    }
  }

  describe("validation of headers") {
    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",

      "headers": [
        %s
      ]
    }
  """

    it("requires name") {
      val json = baseJson.format("""{ "type": "string" }""")
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("All headers must have a name")
    }

    it("requires type") {
      val json = baseJson.format("""{ "name": "no_type" }""")
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("All headers must have a type")
    }

    it("validates type") {
      val json = baseJson.format("""{ "name": "invalid_type", "type": "integer" }""")
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("Header[invalid_type] type[integer] is invalid: Must be a string or the name of an enum")
    }

    it("validates duplicates") {
      val json = baseJson.format("""{ "name": "dup", "type": "string" }, { "name": "dup", "type": "string" }""")
      val validator = ServiceDescriptionValidator(json)
      validator.errors.mkString("") should be("Header[dup] appears more than once")
    }

  }

}
