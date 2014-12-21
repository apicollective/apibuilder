package controllers

import com.gilt.apidocspec.models.json._
import com.gilt.apidocspec.models.{Invocation, Generator, Service}
import generator.{CodeGenTarget, CodeGenerator}
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

object Invocations extends Controller {
  def postByKey(key: String) = Action(parse.json(maxLength = 1024 * 1024)) { request: Request[JsValue] =>
    Generators.findGenerator(key) match {
      case Some((_, generator)) =>
        request.body.validate[Service] match {
          case e: JsError => Conflict(Json.toJson(Validation.invalidJson(e)))
          case s: JsSuccess[Service] => Ok(Json.toJson(Invocation(generator.generate(s.get))))
        }
      case _ => NotFound
    }
  }
}
