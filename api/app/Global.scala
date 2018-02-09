import io.apibuilder.api.v0.models.json._
import lib.Validation
import play.api
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Results._

import scala.concurrent.Future
//import play.api.Play.current

import scala.util.{Failure, Success, Try}

object Global extends WithFilters(LoggingFilter) {

  override def onHandlerNotFound(request: RequestHeader): Future[api.mvc.Results.Status] = {
    Future.successful(NotFound)
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    Future.successful(BadRequest(Json.toJson(Validation.serverError(error))))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    Logger.error(ex.toString, ex)
    Future.successful(InternalServerError(Json.toJson(Validation.serverError(ex.getMessage))))
  }

}
