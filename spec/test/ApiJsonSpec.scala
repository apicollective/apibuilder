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
    description: Option[String] = None,
    fields: Seq[String]
  ): Model = {
    model.description should be(description)
    model.fields.map(_.name) should be(fields)
    model
  }

  def validateField(
    src: Field,
    name: String,
    `type`: String,
    description: scala.Option[String] = None,
    default: scala.Option[_root_.play.api.libs.json.JsObject] = None,
    required: scala.Option[Boolean] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  ) {
    src.name should be(name)
    src.description should be(description)
    src.default should be(default)
    src.`type` should be(`type`)
    src.example should be(example)
    src.minimum should be(minimum)
    src.maximum should be(maximum)
  }

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

        validateBody(service.models("body"))
      }
    }
  }

  def validateBody(body: Model) {
    validateModel(
      body,
      fields = Seq("type", "description")
    )

    validateField(
      body.fields.find(_.name == "type").get,
      name = "type",
      `type` = "string"
    )

    validateField(
      body.fields.find(_.name == "description").get,
      name = "description",
      `type` = "string",
      required = Some(false)
    )
  }

}
