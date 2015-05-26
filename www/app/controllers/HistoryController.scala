package controllers

import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import com.gilt.apidoc.api.v0.errors.UnitResponse

object HistoryController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(
    orgKey: Option[String],
    appKey: Option[String],
    page: Int = 0
  ) = Anonymous.async { implicit request =>
    for {
      changes <- request.api.changes.get(
        orgKey = orgKey,
        applicationKey = appKey,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.history.index(
        request.mainTemplate().copy(title = Some("History")),
        changes = PaginatedCollection(page, changes),
        orgKey = orgKey,
        appKey = appKey
      ))
    }
  }

}
