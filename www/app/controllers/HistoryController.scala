package controllers

import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._
import scala.concurrent.Future

object HistoryController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String, page: Int = 0) = AnonymousOrg.async { implicit request =>
    for {
      changes <- request.api.changes.get(
        orgKey = Some(orgKey),
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
    request.api.applications.getByOrgKey(orgKey, key = Some(applicationKey), limit = 1).flatMap { results =>
      results.headOption match {
        case None => {
          Future {
            Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> "Application not found")
          }
        }
        case Some(app) => {
          for {
            changes <- request.api.changes.get(
              orgKey = Some(orgKey),
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
        }
      }
    }
  }
  
}
