import com.gilt.apidoc.api.v0.models.json._
import actors.MainActor
import db.VersionsDao
import lib.Validation

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Results._
import scala.concurrent.Future
import play.api.Play.current

object Global extends WithFilters(LoggingFilter) {

  override def onStart(app: Application): Unit = {
    global.Actors.mainActor

    import play.api.libs.concurrent.Akka
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global

    Akka.system.scheduler.scheduleOnce(5.seconds) {
      ensureServices()
    }
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest(Json.toJson(Validation.serverError(error))))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error(ex.toString, ex)
    Future.successful(InternalServerError(Json.toJson(Validation.serverError(ex.getMessage))))
  }

  private[this] def ensureServices() {
    Logger.info("Starting ensureServices()")
    val result = VersionsDao.migrate()
    Logger.info("ensureServices() completed: " + result)
  }

}

package global {
  import play.api.libs.concurrent.Akka
  object Actors {
    lazy val mainActor = Akka.system.actorOf(MainActor.props(), "main")
  }
}
