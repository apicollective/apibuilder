package controllers

import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._
import play.api.Logger
import java.util.UUID
import scala.concurrent.Future

object SearchController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(q: Option[String], page: Int = 0) = Anonymous.async { implicit request =>
    for {
      items <- request.api.items.get(
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
