package core

import org.scalatest.{FunSpec, Matchers}

class NestedUnionsSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

    it("validates discriminator values are unique") {
      def setup(userDiscriminatorValue: String, guestDiscriminatorValue: String) = {
        val service = makeApiJson(
          models = Map(
            "user" -> makeModelWithField(),
            "guest" -> makeModelWithField(),
          ),
          unions = Map(
            "abstract_user" -> makeUnion(
              types = Seq(makeUnionType("user", discriminatorValue = Some(userDiscriminatorValue)))
            ),
            "abstract_guest" -> makeUnion(
              types = Seq(makeUnionType("guest", discriminatorValue = Some(guestDiscriminatorValue)))
              ),
            "party" -> makeUnion(
              types = Seq(
                makeUnionType("abstract_user"),
                makeUnionType("abstract_guest"),
              )
            )
          )
        )
        TestHelper.serviceValidator(service).errors()
      }

      setup("user", "guest") shouldBe Nil
      setup("user", "user") shouldBe Seq(
        "TODO"
      )
  }
}
