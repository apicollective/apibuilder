package builder.api_json.templates

import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.Method
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResourceMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  "operations" in {
    val templateOp = makeOperation(
      method = "GET",
      path = "/:id"
    )
    val apiJson = makeApiJson(
      templates = Some(makeTemplates(
        resources = Some(Map(
          "statement" -> makeResource(
            operations = Seq(templateOp)
          )
        ))
      )),
      models = Map(
        "channel_statement" -> makeModel(fields = Seq(makeField("id")))
      ),
      resources = Map(
        "channel_statement" -> makeResource(
          path = Some("/channel/:channel_id/statements"),
          templates = Some(Seq(makeTemplateDeclaration(name = "statement")))
        )
      )
    )

    val op = expectValid(apiJson).resources.head.operations.head
    op.method mustBe Method.Get
    op.path mustBe "/channel/:channel_id/statements/:id"
    op.parameters.map(_.name) mustBe Seq("channel_id", "id")
  }

}
