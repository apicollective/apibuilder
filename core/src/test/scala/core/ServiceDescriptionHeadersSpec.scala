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
        { "name": "X-Foo", "type": "string", "description": "test", "default": "bar" },
        { "name": "X-Bar", "type": "string", "required": false }
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
      ct.default should be(None)
      ct.required should be(true)
      ct.description should be(None)

      val foo = validator.serviceDescription.get.headers.find(_.name == "X-Foo").get
      foo.name should be("X-Foo")
      foo.headertype should be(StringHeaderType)
      foo.default should be(Some("bar"))
      foo.description should be(Some("test"))
      foo.required should be(true)

      val bar = validator.serviceDescription.get.headers.find(_.name == "X-Bar").get
      bar.name should be("X-Bar")
      bar.headertype should be(StringHeaderType)
      bar.default should be(None)
      bar.description should be(None)
      bar.required should be(false)
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
