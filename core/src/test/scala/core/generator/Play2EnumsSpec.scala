package core.generator

import core.ServiceDescription

import core.TestHelper
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

    Play2Enums(service) should be(None)
  }

  it("Valid enum for 1 field with enum") {
    val service = ServiceDescription(json.format("""
{ "name": "age_group", "type": "string", "values": ["Twenties", "Thirties"] }
"""))

    val enums = Play2Enums(service).get

    enums.trim should be("""
package apidoc.enums {

  package user {
    object AgeGroup extends Enumeration {
      type AgeGroup = Value
      val Twenties ,Thirties = Value
    }
  }

}
""".trim)
  }

}
