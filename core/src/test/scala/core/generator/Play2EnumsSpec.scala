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
    enums.trim should be("""
  object User {

    sealed trait AgeGroup

    object AgeGroup {

      case object Twenties extends AgeGroup { override def toString = "twenties" }
      case object Thirties extends AgeGroup { override def toString = "thirties" }

      val AllAgeGroups = Seq(Twenties, Thirties)
      private[this]
      val NameLookup = AllAgeGroups.map(x => x.toString -> x).toMap

      def apply(value: String): Option[AgeGroup] = NameLookup.get(value)

    }

    sealed trait PartyTheme

    object PartyTheme {

      case object Twenties extends PartyTheme { override def toString = "twenties" }
      case object Thirties extends PartyTheme { override def toString = "thirties" }

      val AllPartyThemes = Seq(Twenties, Thirties)
      private[this]
      val NameLookup = AllPartyThemes.map(x => x.toString -> x).toMap

      def apply(value: String): Option[PartyTheme] = NameLookup.get(value)

    }
  }
""".trim)
  }

}
