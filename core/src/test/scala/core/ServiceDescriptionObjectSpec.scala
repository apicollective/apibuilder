package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionObjectSpec extends FunSpec with Matchers {

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

  it("accepts type: object") {
    val json = baseJson.format("""{ "name": "tags", "type": "object" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.head.fields.head
    println("tags: " + tags)
  }

  it("accept defaults for maps") {
    val json = baseJson.format("""{ "name": "tags", "type": "object", "default": "{ }" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.head.fields.head
    tags.default shouldBe Some("{ }")
  }

 /*
 TODO
  it("validates invalid defaults") {
    val json = baseJson.format("""{ "name": "tags", "type": "object", "default": "bar" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Invalid default")
  }
 */

}
