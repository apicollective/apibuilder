package com.gilt.apidocspec

import com.gilt.apidocspec.models._
import com.gilt.apidocspec.models.json._
import play.api.libs.json._

import org.scalatest.{FunSpec, Matchers}

class ApiJsonSpec extends FunSpec with Matchers {

  import Validator._

  val Filenames = Seq("svc-iris-hub-0-0-1.json")
  val Dir = "core/src/test/resources"

  it("spec can parse itself") {
    val contents = scala.io.Source.fromFile("api-json.json").getLines.mkString("\n")

    Json.parse(contents).asOpt[JsValue].getOrElse {
      sys.error("Invalid json")
    }.validate[Service] match {
      case e: JsError => {
        sys.error(e.errors.toString)
      }
      case s: JsSuccess[Service] => {
        val service = s.get
        service.name should be("apidoc spec")
        service.description should be(Some("Specification of apidoc api.json schema"))
        service.baseUrl should be("http://api.apidoc.me")
        service.models.keys.toList.sorted should be(Seq("body", "enum", "enum_value", "field", "header", "model", "operation", "parameter", "resource", "response", "service"))


        validateModel(
          service.models("service"),
          fields = Map(

            "name" -> FieldValidator(
              `type` = "string"
            ),

            "base_url" -> FieldValidator(
              `type` = "string"
            ),

            "description" -> FieldValidator(
              `type` = "string",
              required = Some(false)
            )

          )
        )

        validateBody(service.models("body"))
      }
    }
  }

  def validateBody(body: Model) {
    validateModel(
      body,
      fields = Map(
        "type" -> FieldValidator(
          `type` = "string"
        ),

        "description" -> FieldValidator(
          `type` = "string",
          required = Some(false)
        )
      )
    )
  }

}
