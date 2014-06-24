package core.generator

import core.{ TestHelper, ServiceDescription }
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2EnumsSpec extends FunSpec with ShouldMatchers {

  private lazy val json = """
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


  it("Generates None if service does not have an enum") {
    val service = ServiceDescription(json.format("""
{ "name": "age_group", "type": "string" }
"""))

    val userModel = service.models.head
    Play2Enums.build(userModel) should be(None)
  }

  it("Valid enum for 1 field with enum") {
    val service = ServiceDescription(json.format("""
{ "name": "age_group", "type": "string", "values": ["twenties", "thirties"] },
{ "name": "party_theme", "type": "string", "values": ["twenties", "thirties"] }
"""))

    val userModel = service.models.head
    val enums = Play2Enums.build(userModel).get
    println(enums)

    //enums.trim should be(TestHelper.readFile("core/src/test/files/play2enums-example.txt").trim)
  }

}
