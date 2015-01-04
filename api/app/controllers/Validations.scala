package controllers

import com.gilt.apidoc.models.Validation
import com.gilt.apidoc.models.json._
import core.{ServiceConfiguration, ServiceValidator}
import play.api.mvc._
import play.api.libs.json._

object Validations extends Controller {

  private val config = ServiceConfiguration(
    orgNamespace = "tmp.validations"
  )

  def post() = Action(parse.temporaryFile) { request =>
    val contents = scala.io.Source.fromFile(request.body.file, "UTF-8").getLines.mkString("\n")
    ServiceValidator(config, contents).errors match {
      case Nil => Ok(Json.toJson(Validation(true, Nil)))
      case errors => BadRequest(Json.toJson(Validation(false, errors)))
    }
  }

}
