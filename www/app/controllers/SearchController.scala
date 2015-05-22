package controllers

import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._

object SearchController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String, q: Option[String], page: Int = 0) = AnonymousOrg.async { implicit request =>
    for {
      items <- request.api.items.getSearchByOrgKey(
        orgKey = orgKey,
        q = q,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.search.index(
        request.mainTemplate().copy(
          query = q
        ),
        q = q,
        items = PaginatedCollection(page, items)
      ))
    }
  }
  
}
