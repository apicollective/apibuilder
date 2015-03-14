import com.gilt.apidoc.v0.models.json._
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
    ensureServices()
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

  private def ensureServices() {
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
