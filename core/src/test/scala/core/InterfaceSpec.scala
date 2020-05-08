package core

import io.apibuilder.spec.v0.models.{Interface, Model, Service}
import org.scalatest.{FunSpec, Matchers}

class InterfaceSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val person: Interface = makeInterface(
    name = "person",
    fields = Seq(
      makeField(name = "name")
    )
  )

  private[this] val user: Model = makeModel(
    name = "user",
    interfaces = Seq(person),
    fields = Seq(
      makeField(name = "id")
    )
  )

  private[this] val guest: Model = makeModel(
    name = "guest",
    interfaces = Seq(person),
  )

  private[this] val apiJson = makeService(
    interfaces = Seq(person),
    models = Seq(user, guest),
  )

  private[this] lazy val service = expectValid(apiJson)

  private[this] def expectErrors(service: Service): Seq[String] = {
    TestHelper.serviceValidator(toApiJson(service)).errors()
  }

  private[this] def expectValid(apiJson: Service): Service = {
    val validator = TestHelper.serviceValidator(toApiJson(apiJson))
    if (validator.errors().nonEmpty) {
      sys.error(s"Error: ${validator.errors()}")
    }
    validator.service()
  }

  it("validates that interfaces specified refer to a known interface") {
    expectErrors(
      makeService(models = Seq(user))
    ) should be(
      Seq(s"Model[user] Interface[person] was not found")
    )
  }

  it("validates field types if declared are consistent") {
    expectErrors(
      makeService(models = Seq(user.copy(
        fields = Seq(makeField(name = "name", `type` = "long"))
      )))
    ) should be(
      Seq(s"Model[user] Field[name] type 'long' must be 'string' as defined in the interface 'person'")
    )
  }

  it("models inherit fields") {
    def model(name: String) = service.models.find(_.name == name).getOrElse {
      sys.error(s"Cannot find model: $name")
    }
    model("user").fields.map(_.name) should equal(
      Seq("id", "name")
    )
    model("guest").fields.map(_.name) should equal(
      Seq("name")
    )
  }

  it("model can override description") {
    def model(name: String) = service.models.find(_.name == name).getOrElse {
      sys.error(s"Cannot find model: $name")
    }
    model("user").fields.map(_.name) should equal(
      Seq("id", "name")
    )
    model("guest").fields.map(_.name) should equal(
      Seq("name")
    )
  }

}
