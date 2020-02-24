package io.apicollective.play

import io.apibuilder.api.v0.models.json._
import java.util.UUID

import lib.Validation
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.Logger
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.{InternalServerError, Status}

import scala.concurrent.Future

/**
  * Custom error handler that always returns application/json
  * 
  * Server errors are logged w/ a unique error number that is presented
  * in the message back to the client. This allows us to quickly cross
  * reference an error to a specific point in the log.
  */
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    message.trim match {
      case "" => Future.successful(Status(statusCode))
      case msg => Future.successful(Status(statusCode)(Json.toJson(Validation.error(msg))))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val errorId = UUID.randomUUID().toString.replaceAll("-", "")
    val msg = s"Error [$errorId] ${request.method} ${request.path}: ${exception.getMessage}"

    Logger.error(msg, exception)
    Future.successful(InternalServerError(Json.toJson(Validation.error(msg))))
  }

}
