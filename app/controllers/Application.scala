package controllers

import db.{ Organization, OrganizationQuery, MembershipRequest }
import lib.{ Pagination, PaginatedCollection }

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index(page: Int = 0) = Authenticated { request =>
    val orgs = Organization.findAll(OrganizationQuery(user = Some(request.user),
                                                      limit = Pagination.DefaultLimit+1,
                                                      offset = page * Pagination.DefaultLimit))
    val collection = PaginatedCollection(page, orgs)
    val membershipRequests = MembershipRequest.findAllForUser(request.user)

    Ok(views.html.organizations.index(request.user, collection, membershipRequests))
  }

}
