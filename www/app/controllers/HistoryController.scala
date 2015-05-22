package controllers

import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._

object HistoryController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String, page: Int = 0) = AnonymousOrg.async { implicit request =>
    for {
      changes <- request.api.changes.getByOrgKey(
        orgKey = orgKey,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.history.index(
        request.mainTemplate().copy(title = Some("History")),
        changes = PaginatedCollection(page, changes)
      ))
    }
  }

  def application(orgKey: String, applicationKey: String, page: Int = 0) = AnonymousOrg.async { implicit request =>
    request.api.applications.getByOrgKeyAndKey(orgKey, applicationKey).flatMap { app =>
      for {
        changes <- request.api.changes.getByOrgKey(
          orgKey = orgKey,
          applicationKey = Some(applicationKey),
          limit = Pagination.DefaultLimit+1,
          offset = page * Pagination.DefaultLimit
        )
      } yield {
        Ok(views.html.history.index(
          request.mainTemplate().copy(title = Some("History"), application = Some(app)),
          changes = PaginatedCollection(page, changes)
        ))
      }
    }.recover {
      case com.gilt.apidoc.api.v0.errors.UnitResponse(404) => {
        Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> "Application not found")
      }
    }
  }
  
}
