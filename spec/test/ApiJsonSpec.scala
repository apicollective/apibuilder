package com.gilt.apidocspec

import com.gilt.apidocspec.models._
import com.gilt.apidocspec.models.json._
import play.api.libs.json._

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ApiJsonSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")
  val Dir = "core/src/test/resources"

  case class FieldValidator(
    `type`: String,
    description: scala.Option[String] = None,
    default: scala.Option[_root_.play.api.libs.json.JsObject] = None,
    required: scala.Option[Boolean] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  )

  def validateModel(
    model: Model,
    description: Option[String] = None,
    fields: Map[String, FieldValidator]
  ): Model = {
    model.description should be(description)

    fields.foreach {
      case (name, validator) => validateField(model, name, validator)
    }

    model.fields.map(_.name).sorted should be(fields.keys.toList.sorted)

    model
  }

  def validateField(
    model: Model,
    name: String,
    validator: FieldValidator
  ) {
    val src = model.fields.find(_.name == name).getOrElse {
      sys.error(s"Model does not have a field named[${name}]")
    }

    src.name should be(name)
    src.description should be(validator.description)
    src.default should be(validator.default)
    src.`type` should be(validator.`type`)
    src.example should be(validator.example)
    src.minimum should be(validator.minimum)
    src.maximum should be(validator.maximum)
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

        validateModel(service.models("service"))
        validateBody(service.models("body"))
      }
    }
  }

  def validateModel(model: Model) {
    validateModel(
      model,
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
