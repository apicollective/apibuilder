package controllers

import lib.{ApiClientProvider, PaginatedCollection, Pagination}
import javax.inject.Inject

class HistoryController @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  apiClientProvider: ApiClientProvider
) extends ApibuilderController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

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
      Ok(
        views.html.history.index(
          request.mainTemplate().copy(title = Some("History")),
          changes = PaginatedCollection(page, changes),
          orgKey = orgKey,
          appKey = appKey,
          from = from,
          to = to,
          typ = `type`
        )
      )
    }
  }

}
