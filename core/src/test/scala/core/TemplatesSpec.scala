package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TemplatesSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers {

  it("models inherit fields by name") {
    val name = randomName()
    val templateField = makeField()

    val apiJson = makeApiJson(
      templates = Some(makeTemplates(
        models = Some(Map(
          name -> makeModel(fields = Seq(templateField))
        ))
      )),
      models = Map(name -> makeModel())
    )

    expectValid(apiJson).models.head.fields.map(_.name) shouldBe Seq(templateField.name)
  }

}
