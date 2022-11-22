package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class UnionTypeDiscriminatorValueSpec extends AnyFunSpec with Matchers {

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
    validator.errors() should be(Nil)
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
    validator.errors() should be(Nil)
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
    validator.errors() should be(
      Seq(
        "Union[user] discriminator value[guest_user] appears more than once"
      )
    )
  }

  it("union type discriminator considers only explicit values when present") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "guest_user" },
          |    { "type": "guest_user", "discriminator_value": "guest_user2" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() should be(Nil)
    validator.service().unions.head.types.flatMap(_.discriminatorValue) should be(
      Seq("guest_user", "guest_user2")
    )
  }

  it("union type must be a valid string") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "!@#" },
          |    { "type": "guest_user" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() should be(
      Seq(
        "Union[user] type[registered_user] discriminator_value[!@#] is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters, Name must start with a letter"
      )
    )
  }

  it("union type can share the same models") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "types": [
          |    { "type": "registered_user" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() shouldBe empty

    val sortedUnions = validator.service().unions.sortBy(_.name)
    validator.service().unions should have size 2
    sortedUnions(0).name shouldBe "other_user_union"
    sortedUnions(0).types.flatMap(_.discriminatorValue) shouldBe Seq("registered_user")
    sortedUnions(1).name shouldBe "user"
    sortedUnions(1).types.flatMap(_.discriminatorValue) shouldBe Seq("registered_user", "guest_user")
  }

  it("union type can share the same models if their discriminator value is unique") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() shouldBe empty

    val sortedUnions = validator.service().unions.sortBy(_.name)
    validator.service().unions should have size 2
    sortedUnions(0).name shouldBe "other_user_union"
    sortedUnions(0).types.flatMap(_.discriminatorValue) shouldBe Seq("registered")
    sortedUnions(1).name shouldBe "user"
    sortedUnions(1).types.flatMap(_.discriminatorValue) shouldBe Seq("registered", "guest_user")
  }

  it("union type cannot share the same models if their discriminator value is NOT unique") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "other_registered" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() should be (Seq(
      "Model[registered_user] used in unions[other_user_union, user] cannot use more than one discriminator value. " +
        "Found distinct discriminator values[other_registered, registered]."
    ))
  }

  it("union type can share the same models if their discriminator name is unique") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "discriminator": "key",
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "discriminator": "key",
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" }
          |  ]
          |}
        """.stripMargin
      )
    )
    validator.errors() shouldBe empty

    val sortedUnions = validator.service().unions.sortBy(_.name)
    validator.service().unions should have size 2
    sortedUnions(0).name shouldBe "other_user_union"
    sortedUnions(0).types.flatMap(_.discriminatorValue) shouldBe Seq("registered")
    sortedUnions(1).name shouldBe "user"
    sortedUnions(1).types.flatMap(_.discriminatorValue) shouldBe Seq("registered", "guest_user")
  }

  it("union type cannot share the same models if their discriminator name is NOT unique") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "discriminator": "key",
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "discriminator": "other_key",
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" }
          |  ]
          |}
     """.stripMargin
      )
    )
    validator.errors() should be (Seq(
      "Model[registered_user] used in unions[other_user_union, user] cannot use more than one discriminator name. " +
        "Found distinct discriminator names[other_key, key]."
    ))
  }

  it("union type cannot share the same models if their discriminator key is NOT define for all") {
    val validator = TestHelper.serviceValidatorFromApiJson(
      baseJson.format(
        """
          |"user": {
          |  "discriminator": "key",
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" },
          |    { "type": "guest_user" }
          |  ]
          |},
          |
          |"other_user_union": {
          |  "types": [
          |    { "type": "registered_user", "discriminator_value": "registered" }
          |  ]
          |}
     """.stripMargin
      )
    )
    validator.errors() should be (Seq(
      "Model[registered_user] used in unions[other_user_union, user] cannot use more than one discriminator name. " +
        "All unions should define the same discriminator name, or not define one at all."
    ))
  }

}
