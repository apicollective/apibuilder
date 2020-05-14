package core

import org.scalatest.{FunSpec, Matchers}

class UnionTypeDefaultSpec extends FunSpec with Matchers {

  private[this] val baseJson =
    """
    {
      "name": "Union Types Test",

      %s,

      "models": {

        "registered_user": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        },

        "guest_user": {
          "fields": [
            { "name": "id", "type": "uuid" }
          ]
        }
      }

    }
  """

  describe("without discriminator") {

    val json = baseJson.format(
      """
      "unions": {
         "user": {
           "types": [
             { "type": "registered_user", "default": true }
           ]
         }
      }
      """
    )

    it("reports error if default specified") {
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors should be(
        Seq("Union[user] types cannot specify default as the union type does not have a 'discriminator' specified: registered_user")
      )
    }
  }

  describe("with discriminator") {

    val True = """, "default": true"""
    val False = """, "default": false"""

    val json = baseJson.format(
      """
      "unions": {
         "user": {
           "discriminator": "discriminator",
           "types": [
              { "type": "registered_user"%s },
              { "type": "guest_user"%s }
           ]
         }
      }
      """
    )

    it("Not required to specify a default") {
      val validator = TestHelper.serviceValidatorFromApiJson(
        json.format("", "")
      )
      validator.errors should be(Nil)
    }

    it("Only one type can be the default") {
      val validator = TestHelper.serviceValidatorFromApiJson(
        json.format(True, True)
      )
      validator.errors should be(
        Seq("Union[user] Only 1 type can be specified as default. Currently the following types are marked as default: guest_user, registered_user")
      )
    }

    it("Accepts default false for all types") {
      val validator = TestHelper.serviceValidatorFromApiJson(
        json.format(False, False)
      )
      validator.errors should be(Nil)
      validator.service().unions.find(_.name == "user").get.types.forall(_.default.get == false)
    }

    it("Correctly processes single default") {
      val validator = TestHelper.serviceValidatorFromApiJson(
        json.format(True, False)
      )
      validator.service().unions.find(_.name == "user").get.types.map(_.default.get) should equal(Seq(true, false))
      val types = validator.service().unions.find(_.name == "user").get.types
      types.find(_.`type` == "registered_user").get.default should equal(Some(true))
      types.find(_.`type` == "guest_user").get.default should equal(Some(false))

      val validator2 = TestHelper.serviceValidatorFromApiJson(
        json.format(False, True)
      )
      val types2 = validator2.service().unions.find(_.name == "user").get.types
      types2.find(_.`type` == "registered_user").get.default should equal(Some(false))
      types2.find(_.`type` == "guest_user").get.default should equal(Some(true))
    }
  }

}
