package core

import io.apibuilder.api.json.v0.models.{ApiJson, Interface, Model}
import io.apibuilder.spec.v0.{models => spec}
import org.scalatest.{FunSpec, Matchers}

class InterfaceSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val person: Interface = makeInterface(
    fields = Some(Seq(
      makeField(name = "name")
    ))
  )

  private[this] val user: Model = makeModel(
    interfaces = Some(Seq(person)),
    fields = Seq(
      makeField(name = "id")
    )
  )

  private[this] val guest: Model = makeModel(
    interfaces = Some(Seq(person)),
  )

  private[this] def model(service: spec.Service, name: String): spec.Model = service.models.find(_.name == name).getOrElse {
    sys.error(s"Cannot find model: $name")
  }

  private[this] def expectErrors(apiJson: ApiJson): Seq[String] = {
    TestHelper.serviceValidator(apiJson).errors()
  }

  private[this] def expectValid(apiJson: ApiJson) = {
    val validator = TestHelper.serviceValidator(apiJson)
    if (validator.errors().nonEmpty) {
      sys.error(s"Error: ${validator.errors()}")
    }
    validator.service()
  }

  it("validates that interfaces specified refer to a known interface") {
    expectErrors(
      makeApiJson(models = Map("user" -> user))
    ) should be(
      Seq(s"Model[user] Interface[person] was not found")
    )
  }

  it("validates field types if declared are consistent") {
    expectErrors(
      makeApiJson(models = Map("user" -> user.copy(
        fields = Seq(makeField(name = "name", `type` = "long"))
      )))
    ) should be(
      Seq(s"Model[user] Field[name] type 'long' must be 'string' as defined in the interface 'person'")
    )
  }

  it("models inherit fields") {
    val service = expectValid(
      makeApiJson(
        interfaces = Map("person" -> person),
        models = Map("user" -> user, "guest" -> guest),
      )
    )

    model(service, "user").fields.map(_.name) should equal(
      Seq("id", "name")
    )
    model(service, "guest").fields.map(_.name) should equal(
      Seq("name")
    )
  }

  it("model can override description") {
    val service = expectValid(
      makeApiJson(
        interfaces = Map("person" -> person.copy(description = Some("foo"))),
        models = Map("user" -> user.copy(description = Some("bar")), "guest" -> guest),
      )
    )

    model(service, "user").description should equal(Some("bar"))
    model(service, "guest").description should equal(Some("foo"))
  }

}
