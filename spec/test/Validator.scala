package com.gilt.apidocspec

import com.gilt.apidocspec.models.Model

import org.scalatest.Matchers

object Validator extends Matchers {

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

    // TODO: Test all fielsd
    // model.fields.map(_.name).sorted should be(fields.keys.toList.sorted)

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

}
