package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ServiceDescriptionEnumSpec extends FunSpec with Matchers {

  it("enum with string type") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "string", "enum": ["Twenties", "Thirties"] }
          ]
        }
      }
    }
    """

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val ageGroup = validator.serviceDescription.get.models.head.fields.find { _.name == "age_group" }.get
    ageGroup.fieldtype match {
      case et: EnumerationFieldType => {
        et.values should be(Seq("Twenties", "Thirties"))
      }
      case ft => {
        fail(s"Invalid field type[${ft} for agegroup - should have been an enumeration")
      }
    }
  }

  it("validates that enum type must be string") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "age_group", "type": "integer", "enum": ["Twenties", "Thirties"] }
          ]
        }
      }
    }
    """

    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("Model[user] field[age_group]: enum can only be specified for fields of type 'string'")
  }

}
