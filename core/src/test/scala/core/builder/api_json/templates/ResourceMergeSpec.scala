package builder.api_json.templates

import helpers.ApiJsonHelpers
import io.apibuilder.api.json.v0.models.Operation
import io.apibuilder.spec.v0.models.{Method, Operation => SpecOperation}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResourceMergeSpec extends AnyWordSpec with Matchers with ApiJsonHelpers {

  private[this] def setupOperation(
    templateOp: Operation,
    resourcePath: String = "/partner/statements"
  ): SpecOperation = {
    expectValidApiJson(
      makeApiJson(
        templates = Some(makeTemplates(
          resources = Some(Map(
            "statement" -> makeResource(
              operations = Seq(templateOp)
            )
          ))
        )),
        models = Map(
          "partner_statement" -> makeModel(fields = Seq(makeField("id")))
        ),
        resources = Map(
          "partner_statement" -> makeResource(
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
        makeOperation(path = Some("/:id")),
        resourcePath = "/partner/statements"
      ).path mustBe "/partner/statements/:id"
    }

    "parameters" must {
      "path" in {
        setupOperation(
          makeOperation(path = Some("/:id")),
          resourcePath = "/partner/:partner_id/statements",
        ).parameters.map(_.name) mustBe Seq("partner_id", "id")
      }

      "declared" in {
        val name = "foo"//randomName()
        setupOperation(
          makeOperation(
            path = Some("/:id"),
            parameters = Some(Seq(makeParameter(name = name)))
          )
        ).parameters.map(_.name) mustBe Seq("id", name)
      }
    }
  }

  "response_type is specialized to the specific model" in {
    val templateOp = makeOperation(method = "GET", responses = Some(Map(
      "200" -> makeResponse("statement")
    )))
    val resource = makeResource(
      templates = Some(Seq(makeTemplateDeclaration(name = "statement")))
    )
    val operations = expectValidApiJson(
      makeApiJson(
        templates = Some(makeTemplates(
          resources = Some(Map(
            "statement" -> makeResource(
              operations = Seq(templateOp)
            )
          ))
        )),
        models = Map(
          "partner_statement" -> makeModel(fields = Seq(makeField("id"))),
          "client_statement" -> makeModel(fields = Seq(makeField("id")))
        ),
        resources = Map(
          "partner_statement" -> resource,
          "client_statement" -> resource
        )
      )
    ).resources.flatMap(_.operations).flatMap(_.responses).map(_.`type`).sorted mustBe Seq(
      "client_statement", "partner_statement"
    )
  }

  "body is cast" in {
    def resource(cast: Map[String, String]) = {
      makeResource(
        templates = Some(Seq(makeTemplateDeclaration(
          name = "statement",
          cast = Some(cast)
        )))
      )
    }

    val operations = expectValidApiJson(
      makeApiJson(
        templates = Some(makeTemplates(
          models = Some(Map(
            "statement_form" -> makeModel(fields = Seq(makeField()))
          )),
          resources = Some(Map(
            "statement" -> makeResource(
              operations = Seq(
                makeOperation(method = "POST", body = Some(makeBody(`type` = "statement_form")))
              )
            )
          ))
        )),
        models = Map(
          "partner_statement_form" -> makeModel(templates = Some(Seq(makeTemplateDeclaration(name = "statement_form")))),
          "partner_statement" -> makeModel(fields = Seq(makeField("id"))),
          "client_statement_form" -> makeModel(templates = Some(Seq(makeTemplateDeclaration(name = "statement_form")))),
          "client_statement" -> makeModel(fields = Seq(makeField("id"))),
        ),
        resources = Map(
          "partner_statement" -> resource(
            Map("statement" -> "partner_statement", "statement_form" -> "partner_statement_form")
          ),
          "client_statement" -> resource(
            Map("statement" -> "client_statement", "statement_form" -> "client_statement_form")
          ),
        )
      )
    ).resources.flatMap(_.operations).flatMap(_.body).map(_.`type`).sorted mustBe Seq(
      "client_statement_form", "partner_statement_form"
    )
  }
}
