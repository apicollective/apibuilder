package core

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Model
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TemplateSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private[this] def makeModelWithTemplate(templateName: String): Model = {
    makeModel(templates = Some(Seq(makeTemplateDeclaration(name = templateName))))
  }

  it("model and template.models cannot have the same name") {
    expectErrors(
      makeApiJson(
        templates = Some(makeTemplates(models = Some(Map(
          "person" -> makeModel()
        )))),
        models = Map("person" -> makeModelWithField()),
      )
    ) should be(
      Seq("Name[person] cannot be used as the name of both a model and a template model")
    )
  }

  it("interface and template.models cannot have the same name") {
    expectErrors(
      makeApiJson(
        templates = Some(makeTemplates(models = Some(Map(
          "person" -> makeModel()
        )))),
        interfaces = Map("person" -> makeInterface()),
      )
    ) should be(
      Seq("Name[person] cannot be used as the name of both an interface and a template model")
    )
  }

  it("inherits templates defined on the template itself") {
    val fieldName = randomName()
    val modelName = randomName()

    expectValid(
      makeApiJson(
        templates = Some(makeTemplates(
          models = Some(Map(
            "user" -> makeModel(fields = Seq(makeField(name = fieldName))),
            "person" -> makeModelWithTemplate("user")
          ))
        )),
        models = Map(
          modelName -> makeModelWithTemplate("person")
        )
      )
    ).models.head.fields.map(_.name) shouldBe Seq(fieldName)
  }

  it("duplicates cycles") {
    expectErrors(
      makeApiJson(
        templates = Some(makeTemplates(models = Some(Map(
          "person" -> makeModel(
            templates = Some(Seq(makeTemplateDeclaration(name = "person")))
          )
        )))),
        models = Map(
          randomName() -> makeModelWithTemplate("person")
        )
      )
    ) should be(
      Seq("Name[person] TODO")
    )
  }

}
