import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends WithFilters(LoggingFilter) {

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request: " + error))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(ex.toString, ex)
    Future.successful(InternalServerError(ex.toString))
  }

}
