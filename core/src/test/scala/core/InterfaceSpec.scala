package core

import io.apibuilder.api.json.v0.models.{ApiJson, Field, Interface, Model}
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

  it("validates interface name") {
    expectErrors(
      makeApiJson(interfaces = Map("  " -> makeInterface()))
    ) should be(
      Seq("Interface[  ] name cannot be empty")
    )

    expectErrors(
      makeApiJson(interfaces = Map("!" -> makeInterface()))
    ) should be(
      Seq("Interface[!] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters and Name must start with a letter")
    )
  }

  it("supports interface with no fields") {
    expectValid(
      makeApiJson(interfaces = Map("test" -> makeInterface(fields = None)))
    )
  }

  it("validates fields") {
    def test(field: Field) = {
      expectErrors(
        makeApiJson(
          interfaces = Map("test" -> makeInterface(
            fields = Some(Seq(field))
          ))
        )
      )
    }
    test(makeField(name = "!")) should be(
      Seq("Interface[test] Field[!] name is invalid: Name can only contain a-z, A-Z, 0-9, - and _ characters and Name must start with a letter")
    )
    test(makeField(name = "id", `type` = "foo")) should be(
      Seq("Interface[test] Field[id] type 'foo' was not found")
    )
  }

  it("validates that interfaces specified refer to a known interface") {
    expectErrors(
      makeApiJson(models = Map("user" -> user))
    ) should be(
      Seq("Model[user] Interface[person] was not found")
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
