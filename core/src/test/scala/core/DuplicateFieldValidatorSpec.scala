package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DuplicateFieldValidatorSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

    it("detects duplicate fields") {
      def setup(name1: String, name2: String) = {
        TestHelper.serviceValidatorFromApiJson(
          s"""
            |{
            |  "name": "duplicate-test",
            |  "models": {
            |    "$name1": {
            |      "fields": [{"name": "placeholder", "type": "string"}]
            |    },
            |    "$name2": {
            |      "fields": [{"name": "placeholder", "type": "string"}]
            |    }
            |  }
            |}
            |""".stripMargin
        ).errors()
      }

      setup("user1", "user2") shouldBe Nil
      setup("user", "user") shouldBe Seq("Invalid json, duplicate key name found: user")
    }
}
