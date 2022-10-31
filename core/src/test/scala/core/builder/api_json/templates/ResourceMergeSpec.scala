package builder.api_json.templates

import helpers.ApiJsonHelpers
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResourceMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  "operations" in {
    val op = makeOperation()
    val apiJson = makeApiJson(
      templates = Some(makeTemplates(
        resources = Some(Map(
          "statement" -> makeResource(
            operations = Seq(op)
          )
        ))
      )),
      models = Map(
        "channel_statement" -> makeModel(fields = Seq(makeField("id")))
      ),
      resources = Map(
        "channel_statement" -> makeResource(
          templates = Some(Seq(makeTemplateDeclaration(name = "statement")))
        )
      )
    )

    expectValid(apiJson).resources.head.operations mustBe Seq(op)
  }

}
