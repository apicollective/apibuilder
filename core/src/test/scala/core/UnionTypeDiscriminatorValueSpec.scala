package core

import org.scalatest.{FunSpec, Matchers}

class UnionTypeDiscriminatorValueSpec extends FunSpec with Matchers {

  private[this] val baseJson =
    """
    {
      "name": "Union Types Test",

      "unions": {
        %s
      },

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

  it("union type discriminator value defaults to the type name") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user" },
          |    { "type": "guest_user" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors should be(Nil)
    validator.service().unions.head.types.flatMap(_.discriminatorValue) should be(
      Seq("registered_user", "guest_user")
    )
  }

  it("union type discriminator value is respected when provided") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors should be(Nil)
    validator.service().unions.head.types.flatMap(_.discriminatorValue) should be(
      Seq("registered", "guest_user")
    )
  }

  it("union type discriminator must be unique") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "guest_user" },
          |    { "type": "guest_user" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors should be(
      Seq(
        "TODO: Discriminator value guest_user specified more than once"
      )
    )
  }

}
