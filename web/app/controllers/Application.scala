package controllers

import client.Apidoc
import lib.{ Pagination, PaginatedCollection }

import play.api._
import play.api.mvc._

object Application extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgsPage: Int = 0, membershipRequestsPage: Int = 0) = Authenticated.async { implicit request =>
    for {
      orgs <- request.client.organizations.findAll(userGuid = request.user.guid,
                                                   limit = Pagination.DefaultLimit+1,
                                                   offset = orgsPage * Pagination.DefaultLimit)
      membershipRequests <- request.client.membershipRequests.findAll(userGuid = Some(request.user.guid),
                                                                      limit = Pagination.DefaultLimit+1,
                                                                      offset = membershipRequestsPage * Pagination.DefaultLimit)
    } yield {
      Ok(views.html.index(request.user,
                          PaginatedCollection(orgsPage, orgs),
                          PaginatedCollection(membershipRequestsPage, membershipRequests)))
    }
  }

}
