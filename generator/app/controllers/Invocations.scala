package controllers

import com.gilt.apidocgenerator.models.json._
import com.gilt.apidocgenerator.models.{Invocation, Generator, ServiceDescription}
import generator.{CodeGenTarget, CodeGenerator}
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

object Invocations extends Controller {
  def postByKey(key: String) = Action(parse.json(maxLength = 1024 * 1024)) { request: Request[JsValue] =>
    Generators.findGenerator(key) match {
      case Some((_, generator)) =>
        request.body.validate[ServiceDescription] match {
          case e: JsError => Conflict(Json.toJson(Validation.invalidJson(e)))
          case s: JsSuccess[ServiceDescription] => Ok(Json.toJson(Invocation(generator.generate(s.get))))
        }
      case _ => NotFound
    }
  }
}
