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
{ "name": "age_group", "type": "string", "values": ["Twenties", "Thirties"] },
{ "name": "party_theme", "type": "string", "values": ["Twenties", "Thirties"] }
"""))

    val enums = Play2Enums(service).get

    enums.trim should be("""
package apidoc.enums {

  object User {

    sealed trait AgeGroup

    object AgeGroup {

      case object Twenties extends AgeGroup
      case object Thirties extends AgeGroup

      val AllAgeGroups = Seq(Twenties, Thirties)
      private[this]
      val NameLookup = AllAgeGroups.map(x => x.toString -> x).toMap

      def apply(value: String): Option[AgeGroup] = NameLookup.get(value)

    }

  }

  object User {

    sealed trait PartyTheme

    object PartyTheme {

      case object Twenties extends PartyTheme
      case object Thirties extends PartyTheme

      val AllPartyThemes = Seq(Twenties, Thirties)
      private[this]
      val NameLookup = AllPartyThemes.map(x => x.toString -> x).toMap

      def apply(value: String): Option[PartyTheme] = NameLookup.get(value)

    }

  }

}
""".trim)
  }

}
