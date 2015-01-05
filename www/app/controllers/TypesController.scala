package controllers

import lib.TypeNameResolver

import play.api._
import play.api.mvc._

object TypesController extends Controller {

  def resolve(typeName: String) = Action { implicit request =>
    TypeNameResolver(typeName).resolve match {
      case None => {
        sys.error("typeName: " + typeName + " could not be resolved")
      }
      case Some(resolution) => {
        sys.error(resolution.toString)
      }
    }
  }

}
