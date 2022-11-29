package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeDefaultSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

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
      TestHelper.expectSingleError(json) should be(
        "Union[user] types cannot specify default as the union type does not have a 'discriminator' specified: registered_user"
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
      setupValidApiJson(json.format("", ""))
    }

    it("Only one type can be the default") {
      TestHelper.expectSingleError(
        json.format(True, True)
      ) should be(
        "Union[user] Only 1 type can be specified as default. Currently the following types are marked as default: guest_user, registered_user"
      )
    }

    it("Accepts default false for all types") {
      setupValidApiJson(
        json.format(False, False)
      ).unions.find(_.name == "user").get.types.forall(_.default.get == false)
    }

    it("Correctly processes single default") {
      val service = setupValidApiJson(
        json.format(True, False)
      )
      service.unions.find(_.name == "user").get.types.map(_.default.get) should equal(Seq(true, false))
      val types = service.unions.find(_.name == "user").get.types
      types.find(_.`type` == "registered_user").get.default should equal(Some(true))
      types.find(_.`type` == "guest_user").get.default should equal(Some(false))

      val types2 = setupValidApiJson(
        json.format(False, True)
      ).unions.find(_.name == "user").get.types
      types2.find(_.`type` == "registered_user").get.default should equal(Some(false))
      types2.find(_.`type` == "guest_user").get.default should equal(Some(true))
    }
  }

}
