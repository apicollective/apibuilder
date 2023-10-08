package controllers

import lib.{PaginatedCollection, Pagination}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HistoryController @Inject() (
                                    val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

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
