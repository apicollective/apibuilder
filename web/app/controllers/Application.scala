package controllers

import core.OrganizationQuery
import lib.{ Pagination, PaginatedCollection }

import play.api._
import play.api.mvc._



object Application extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Authenticated.async { request =>
    for {
      orgs <- Apidoc.organizations.findAll(OrganizationQuery(user_guid = request.user.guid,
                                                             limit = Pagination.DefaultLimit+1,
                                                             offset = page * Pagination.DefaultLimit))
      //val membershipRequests = MembershipRequest.findAllForUser(request.user)
    } yield {
      val collection = PaginatedCollection(page, orgs)
      // Ok(views.html.organizations.index(request.user, collection, membershipRequests))
      Ok("ok")
    }
  }

}
