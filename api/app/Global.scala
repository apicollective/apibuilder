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

  override def onStart(app: Application): Unit = {
//    import play.api.libs.concurrent.Akka
//    import scala.concurrent.duration._
//    import scala.concurrent.ExecutionContext.Implicits.global

    app.mode match {
      case Mode.Test => {
        // No-op. Skip call to ensure services while testing
      }
      case Mode.Prod | Mode.Dev => {
        //Akka.system.scheduler.scheduleOnce(5.seconds) {
        //  ensureServices()
        //}
      }
    }
  }

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

  private[this] def ensureServices() {
    // TODO: Move to background actor and out of global
    Logger.info("Starting ensureServices()")

    Try {
      Logger.warn("Migration disabled")
      play.api.Play.current.injector.instanceOf[_root_.db.VersionsDao].migrate()
    } match {
      case Success(result) => Logger.info("ensureServices() completed: " + result)
      case Failure(ex) => Logger.error(s"Error migrating versions: ${ex.getMessage}")
    }
  }

}
