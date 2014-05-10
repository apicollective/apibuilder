package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ReferenceSpec extends FunSpec with Matchers {

  it("looks up type when referencing an existing field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "models": {
        "user": {
          "fields": [
            { "name": "guid", "type": "uuid" }
          ]
        },
        "account": {
          "fields": [
            { "name": "user", "references": "user.guid" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val accounts = validator.serviceDescription.get.models.find { _.name == "accounts" }.get
    accounts.fields.head.name should be("user")
    accounts.fields.head.datatype.name should be("uuid")
  }

}
