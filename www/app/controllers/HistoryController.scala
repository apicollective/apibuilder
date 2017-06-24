package controllers

import lib.{Pagination, PaginatedCollection}
import scala.concurrent.Future
import io.apibuilder.apidoc.api.v0.errors.UnitResponse

import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class HistoryController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(
    orgKey: Option[String],
    appKey: Option[String],
    from: Option[String],
    to: Option[String],
    `type`: Option[String],
    page: Int = 0
  ) = Anonymous.async { implicit request =>
    for {
      changes <- request.api.changes.get(
        orgKey = orgKey,
        applicationKey = appKey,
        from = from,
        to = to,
        `type` = `type`,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.history.index(
        request.mainTemplate().copy(title = Some("History")),
        changes = PaginatedCollection(page, changes),
        orgKey = orgKey,
        appKey = appKey,
        from = from,
        to = to,
        typ = `type`
      ))
    }
  }

}
