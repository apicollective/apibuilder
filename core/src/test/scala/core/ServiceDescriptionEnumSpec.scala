package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionEnumSpec extends FunSpec with Matchers {

    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "enums": {
        "age_group": {
          "values": [
            { "name": "Twenties" },
            { "name": "Thirties" }%s
          ]
        }
      },

      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "age_group" }
          ]
        }
      }
    }
    """

  it("field can be defined as an enum") {
    val json = baseJson.format("")
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val ageGroup = validator.serviceDescription.get.models.head.fields.find { _.name == "age_group" }.get
    ageGroup.fieldtype match {
      case et: EnumFieldType => {
        et.enum.name should be("age_group")
        et.enum.values.map(_.name) should be(Seq("Twenties", "Thirties"))
      }
      case ft => {
        fail(s"Invalid field type[${ft}] for age_group - should have been an enumeration")
      }
    }
  }

  it("validates that enum values must be valid symbols") {
    val json = baseJson.format(""", { "name": "1" } """)
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Enum[age_group] value[1] is invalid: Name must start with a letter")
  }

}
