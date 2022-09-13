package core

import org.scalatest.{FunSpec, Matchers}

class DuplicateFieldValidatorSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

    it("detects duplicate fields") {
      TestHelper.serviceValidatorFromApiJson(
        """
          |{
          |  "name": "duplicate-test",
          |  "models": {
          |    "user": {
          |      "fields": [{"name": "placeholder", "type": "string"}]
          |    },
          |    "user": {
          |      "fields": [{"name": "placeholder", "type": "string"}]
          |    }
          |  }
          |}
          |""".stripMargin
      ).errors() shouldBe Seq("Duplicate")
    }
}
