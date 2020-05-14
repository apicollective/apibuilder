package core

import io.apibuilder.api.json.v0.models.{ApiJson, Field, Interface, Model}
import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.{models => spec}
import org.scalatest.{FunSpec, Matchers}

class InterfaceSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] val person: Interface = makeInterface(
    fields = Some(Seq(
      makeField(name = "name", required = true)
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

  private[this] lazy val servicePersonUnionAndInterface: Service = expectValid(
    makeApiJson(
      interfaces = Map("person" -> person),
      unions = Map("person" -> makeUnion(types = Seq(makeUnionType("user")))),
      models = Map("user" -> makeModel()),
    )
  )

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
      Seq("Interface[test] Field[id] type[foo] not found")
    )
  }

  it("validates that interfaces specified refer to a known interface") {
    expectErrors(
      makeApiJson(models = Map("user" -> user))
    ) should be(
      Seq("Model[user] Interface[person] not found")
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

  it("validates field 'required' if declared is consistent") {
    expectErrors(
      makeApiJson(
        interfaces = Map("person" -> person),
        models = Map("user" -> user.copy(
          fields = Seq(makeField(name = "name", required = false))
        ))
      )
    ) should be(
      Seq(s"Model[user] field 'name' cannot be optional. Must match the 'person' interface which defines this field as required")
    )
  }

  it("union and interface can have the same name (both essentially define interfaces and can be combined in generators)") {
    servicePersonUnionAndInterface.unions.head.name should be("person")
    servicePersonUnionAndInterface.unions.head.interfaces should be(Seq("person"))
    servicePersonUnionAndInterface.interfaces.head.name should be("person")
  }

  it("union automatically lists any interface matching its name") {
    servicePersonUnionAndInterface.unions.head.interfaces should be(Seq("person"))
  }

  it("model and interface cannot have the same name") {
    expectErrors(
      makeApiJson(
        interfaces = Map("person" -> person),
        models = Map("person" -> makeModel()),
      )
    ) should be(
      Seq("Name[person] cannot be used as the name of both a model and an interface type")
    )
  }

}
