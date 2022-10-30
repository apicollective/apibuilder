package core

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TemplateSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

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

}
