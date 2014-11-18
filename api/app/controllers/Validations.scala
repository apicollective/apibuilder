package controllers

import com.gilt.apidoc.models.Validation
import core.ServiceDescriptionValidator
import play.api.mvc._
import play.api.libs.json._

object Validations extends Controller {

  def post() = Action(parse.temporaryFile) { request =>
    val contents = scala.io.Source.fromFile(request.body.file).getLines.mkString("\n")
    ServiceDescriptionValidator(contents).errors match {
      case Nil => Ok(Json.toJson(Validation(true, Nil)))
      case errors => Ok(Json.toJson(Validation(false, errors)))
    }
  }

}
