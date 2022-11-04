package builder.api_json.templates

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Operation
import io.apibuilder.spec.v0.models.{Method, Operation => SpecOperation}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResourceMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  private[this] def setupOperation(
    templateOp: Operation,
    resourcePath: String = "/channel/statements"
  ): SpecOperation = {
    expectValid(
      makeApiJson(
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
            path = Some(resourcePath),
            templates = Some(Seq(makeTemplateDeclaration(name = "statement")))
          )
        )
      )
    ).resources.head.operations.head
  }

  "operations" must {
    "method" in {
      setupOperation(
        makeOperation(method = "GET")
      ).method mustBe Method.Get
    }

    "path" in {
      setupOperation(
        makeOperation(path = "/:id"),
        resourcePath = "/channel/statements"
      ).path mustBe "/channel/statements/:id"
    }

    "parameters" must {
      "path" in {
        setupOperation(
          makeOperation(path = "/:id"),
          resourcePath = "/channel/:channel_id/statements",
        ).parameters.map(_.name) mustBe Seq("channel_id", "id")
      }

      "declared" in {
        val name = randomName()
        setupOperation(
          makeOperation(
            path = "/:id",
            parameters = Some(Seq(makeParameter(name = name)))
          )
        ).parameters.map(_.name) mustBe Seq("id", name)
      }
    }
  }

  "response_type is specialized to the specific model" in {
    setupOperation(
      makeOperation(
        responses = Some(Map("200" -> makeResponse("statement")))
      )
    ).responses.map(_.`type`) mustBe "channel_statement"
  }

}
