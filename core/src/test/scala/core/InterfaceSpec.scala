package core

import io.apibuilder.api.json.v0.models.{ApiJson, Field, Interface, Model}
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

  private[this] def servicePersonUnionAndInterface(
    interfaces: Seq[String],
    userModel: Model = makeModel()
  ): ApiJson = makeApiJson(
    interfaces = Map("person" -> person),
    unions = Map("person" -> makeUnion(
      interfaces = Some(interfaces),
      types = Seq(makeUnionType("user")))
    ),
    models = Map("user" -> userModel),
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

  it("validates field type is not an interface") {
    expectErrors(
      makeApiJson(
        interfaces = Map("person" -> person),
        models = Map("user" -> makeModel(
          fields = Seq(
            makeField(name = "id", `type` = "person")
          )
        ))
      )
    ) should be(
      Seq("Model[user] Field[id] type[person] is an interface and cannot be used as a field type. Specify the specific model you need or use a union type")
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
    val svc = expectValid {
      servicePersonUnionAndInterface(Seq("person"))
    }
    svc.unions.head.name should be("person")
    svc.unions.head.interfaces should be(Seq("person"))
    svc.interfaces.head.name should be("person")
  }

  it("union and interface can have the same name only if interface is specified") {
    expectErrors {
      servicePersonUnionAndInterface(Nil)
    } should be(
      Seq("'person' is defined as both a union and an interface. You must either make the names unique, or document in the union interfaces field that the type extends the 'person' interface.")
    )
  }

  it("model fields can reference a union w /interface") {
    val svc = expectValid {
      servicePersonUnionAndInterface(
        interfaces = Seq("person"),
        userModel = makeModel(
          interfaces = Some(Seq("person")),
          fields = person.fields.getOrElse(Nil)
        )
      )
    }
    svc.models.head.interfaces shouldBe Seq("person")
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
