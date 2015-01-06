package controllers

import lib.TypeNameResolver

import play.api._
import play.api.mvc._
import scala.concurrent.Future

object TypesController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def resolve(typeName: String) = Anonymous.async { implicit request =>
    TypeNameResolver(typeName).resolve match {
      case None => Future {
        Ok(views.html.types.resolve(
          request.mainTemplate(),
          typeName
        ))
      }
      case Some(resolution) => {
        sys.error(resolution.toString)
      }
    }
  }

}
