package com.gilt.apidocspec

import com.gilt.apidocspec.models._
import com.gilt.apidocspec.models.json._
import play.api.libs.json._

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ApiJsonSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")
  val Dir = "core/src/test/resources"

  def validateModel(
    model: Model,
    description: Option[String],
    fields: Seq[String]
  ): Model = {
    model.description should be(description)
    model.fields.map(_.name) should be(fields)
    model
  }

  def validateField(src: Field, target: Field) {
    src.name should be(target.name)
    src.description should be(target.description)
    src.`type` should be(target.`type`)
    src.example should be(target.example)
    src.minimum should be(target.minimum)
    src.maximum should be(target.maximum)
  }

  it("spec can parse itself") {
    val contents = scala.io.Source.fromFile("api-json.json").getLines.mkString("\n")
    Json.parse(contents).asOpt[Service] match {
      case None => sys.error("Failed to parse")
      case Some(service) => {
        service.name should be("apidoc spec")
        service.description should be(Some("Specification of apidoc api.json schema"))
        service.baseUrl should be("http://api.apidoc.me")
        service.models.keys.toList.sorted should be(Seq("body", "enum", "enum_value", "field", "header", "model", "operation", "parameter", "resource", "response", "service"))

        val body = validateModel(
          service.models("body"),
          description = None,
          fields = Seq("type", "description")
        )

        validateField(
          body.fields.find(_.name == "type").get,
          Field(
            name = "type",
            `type` = "string",
            required = None,
            example = None,
            minimum = None,
            maximum = None
          )
        )

        validateField(
          body.fields.find(_.name == "description").get,
          Field(
            name = "description",
            `type` = "string",
            required = Some(false),
            example = None,
            minimum = None,
            maximum = None
          )
        )
      }
    }
 }

}
