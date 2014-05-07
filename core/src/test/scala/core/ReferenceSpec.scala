package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ReferenceSpec extends FunSpec with Matchers {

  it("looks up type when referencing an existing field") {
    val json = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc",
      "resources": {
        "users": {
          "fields": [
            { "name": "guid", "type": "string", "format": "uuid" }
          ]
        },
        "accounts": {
          "fields": [
            { "name": "user_guid", "references": "users.guid" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val accounts = validator.serviceDescription.get.resources.find { _.name == "accounts" }.get
    accounts.fields.head.name should be("user_guid")
    accounts.fields.head.datatype.name should be("string")
    accounts.fields.head.format.get should be("uuid")
  }

}
