package core

import cats.data.Validated.{Invalid, Valid}
import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class NestedUnionsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  it("validates discriminator values are unique") {
    def setup(
               userDiscriminatorValue: String,
               guestDiscriminatorValue: String,
               abstractUserDiscriminatorValue: Option[String],
             ) = {
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
              makeUnionType("abstract_user", discriminatorValue = abstractUserDiscriminatorValue),
              makeUnionType("abstract_guest"),
            )
          )
        )
      )
      TestHelper.serviceValidator(service) match {
        case Invalid(e) => e.toNonEmptyList.toList
        case Valid(_) => Nil
      }
    }

    setup("user", "guest", None) shouldBe Nil
    setup("user", "user", None) shouldBe Seq(
      "Union[party] discriminator value[user] appears more than once"
    )
    setup("user", "guest", Some("user")) shouldBe Seq(
      "Union[party] discriminator value[user] appears more than once"
    )
  }

  it("share a common discriminator") {
    def setup(
      userDiscriminatorName: Option[String],
      guestDiscriminatorName: Option[String],
      partyDiscriminatorName: Option[String],
    ) = {
      val service = makeApiJson(
        models = Map(
          "user" -> makeModelWithField(),
          "guest" -> makeModelWithField(),
        ),
        unions = Map(
          "abstract_user" -> makeUnion(
            discriminator = userDiscriminatorName,
            types = Seq(makeUnionType("user"))
          ),
          "abstract_guest" -> makeUnion(
            discriminator = guestDiscriminatorName,
            types = Seq(makeUnionType("guest")),
          ),
          "party" -> makeUnion(
            discriminator = partyDiscriminatorName,
            types = Seq(
              makeUnionType("abstract_user"),
              makeUnionType("abstract_guest"),
            )
          )
        )
      )
      TestHelper.serviceValidator(service) match {
        case Invalid(e) => e.toNonEmptyList.toList
        case Valid(_) => Nil
      }
    }

    setup(None, None, None) shouldBe Nil
    setup(Some("discriminator"), Some("discriminator"), Some("discriminator")) shouldBe Nil
    setup(Some("discriminator"), None, None) shouldBe Seq(
      "Union[party] does not specify a discriminator yet one of the types does. Either add the same discriminator to this union or remove from the member types"
    )
    setup(None, None, Some("discriminator")) shouldBe Seq(
      "Union[party] specifies a discriminator named 'discriminator'. All member types must also specify this same discriminator"
    )
    setup(Some("foo"), Some("discriminator"), Some("discriminator")) shouldBe Seq(
      "Union[party] specifies a discriminator named 'discriminator'. All member types must also specify this same discriminator"
    )
  }
}
