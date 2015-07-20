package controllers

import lib.{Pagination, PaginatedCollection}
import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class SearchController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {
  
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(q: Option[String], org: Option[String], page: Int = 0) = Anonymous.async { implicit request =>
    val finalQuery = Seq(
      org.map { key => s"org:$key" },
      q
    ).filter(!_.isEmpty).flatten.mkString(" ")

    for {
      items <- request.api.items.get(
        q = Some(finalQuery),
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.search.index(
        request.mainTemplate().copy(
          query = Some(finalQuery)
        ),
        q = q,
        org = org,
        items = PaginatedCollection(page, items)
      ))
    }
  }
  
}
