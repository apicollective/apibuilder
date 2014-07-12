package controllers

import lib.{ Pagination, PaginatedCollection }
import java.util.UUID

import play.api._
import play.api.mvc._

object Application extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgsPage: Int = 0, membershipRequestsPage: Int = 0) = Authenticated.async { implicit request =>
    for {
      orgs <- request.api.Organizations.get(userGuid = Some(request.user.guid),
                                                     limit = Some(Pagination.DefaultLimit+1),
                                                     offset = Some(orgsPage * Pagination.DefaultLimit))
      membershipRequests <- request.api.MembershipRequests.get(userGuid = Some(request.user.guid),
                                                               limit = Some(Pagination.DefaultLimit+1),
                                                               offset = Some(membershipRequestsPage * Pagination.DefaultLimit))
    } yield {
      Ok(views.html.index(request.user,
                          PaginatedCollection(orgsPage, orgs),
                          PaginatedCollection(membershipRequestsPage, membershipRequests)))
    }
  }

}
