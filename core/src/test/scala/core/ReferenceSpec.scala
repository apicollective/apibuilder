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
            { "name": "user", "references": "users.guid" }
          ]
        }
      }
    }
    """
    val validator = ServiceDescriptionValidator(json)
    validator.errors.mkString("") should be("")
    val account = validator.serviceDescription.get.models.find { _.name == "account" }.get
    account.fields.head.name should be("user")
    account.fields.head.datatype.name should be("uuid")
  }

}
