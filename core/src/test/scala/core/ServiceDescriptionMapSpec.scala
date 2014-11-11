package core

import com.gilt.apidocgenerator.models.{Type, TypeKind}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionMapSpec extends FunSpec with Matchers {

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

  it("accepts type: map") {
    val json = baseJson.format("""{ "name": "tags", "type": "map" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.head.fields.head
    tags.datatype should be(Type(TypeKind.Primitive, Datatype.MapType.name, false))
  }

  it("accept defaults for maps") {
    val json = baseJson.format("""{ "name": "tags", "type": "map", "default": "{ }" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val tags = validator.serviceDescription.get.models.head.fields.head
    tags.default shouldBe Some("{ }")
  }

  it("validates invalid defaults") {
    val json = baseJson.format("""{ "name": "tags", "type": "map", "default": "bar" }""")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Model[user] field[tags] Default[bar] is not valid for datatype[map]")
  }

}
