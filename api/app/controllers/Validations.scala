package controllers

import core.ServiceDescriptionValidator
import play.api.mvc._
import play.api.libs.json._

object Validations extends Controller {

  case class ValidationResult(valid: Boolean, errors: Seq[String])

  object ValidationResult {
    implicit val validationResultWrites = Json.writes[ValidationResult]
  }

  def post() = Action(parse.temporaryFile) { request =>
    val contents = scala.io.Source.fromFile(request.body.file).getLines.mkString("\n")
    ServiceDescriptionValidator(contents).errors match {
      case Nil => Ok(Json.toJson(ValidationResult(true, Nil)))
      case errors => Ok(Json.toJson(ValidationResult(false, errors)))
    }
  }

}
