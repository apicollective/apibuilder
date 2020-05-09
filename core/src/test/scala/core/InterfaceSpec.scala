package core

import io.apibuilder.api.json.v0.models.{ApiJson, Interface, Model}
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.spec.v0.{models => spec}
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class InterfaceSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val person: Interface = makeInterface(
    fields = Some(Seq(
      makeField(name = "name")
    ))
  )

  private[this] val user: Model = makeModel(
    interfaces = Some(Seq("person")),
    fields = Seq(
      makeField(name = "id")
    )
  )

  private[this] val guest: Model = makeModel(
    interfaces = Some(Seq("person")),
    fields = Seq(
      makeField(name = "age")
    )
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
      Seq(s"Model[user] interface[person] was not found")
    )
  }

  it("validates field types if declared are consistent") {
    expectErrors(
      makeApiJson(
        interfaces = Map("person" -> person),
        models = Map("user" -> user.copy(
          fields = Seq(makeField(name = "name", `type` = "long"))
        ))
      )
    ) should be(
      Seq(s"Model[user] field 'name' type 'long' is invalid. Must match the 'person' interface which defines this field as type 'string'")
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
      Seq("age", "name")
    )
  }

}
