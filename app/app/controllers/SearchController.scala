package controllers

import lib.{PaginatedCollection, Pagination, Util}
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SearchController @Inject() (
                                   val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                   util: Util
) extends ApiBuilderController {
  
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def index(q: Option[String], org: Option[String], page: Int = 0): Action[AnyContent] = Anonymous.async { implicit request =>
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
