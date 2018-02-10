package controllers

import lib.{ApiClientProvider, PaginatedCollection, Pagination, Util}
import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

class SearchController @Inject() (
  val messagesApi: MessagesApi,
  apiClientProvider: ApiClientProvider,
  util: Util
) extends Controller with I18nSupport {
  
  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def index(q: Option[String], org: Option[String], page: Int = 0) = Anonymous.async { implicit request =>
    val finalQuery = Seq(
      org.map { key => s"org:$key" },
      q
    ).filter(_.isDefined).flatten.mkString(" ")

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
        util,
        q = q,
        org = org,
        items = PaginatedCollection(page, items)
      ))
    }
  }
  
}
