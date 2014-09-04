package controllers

import lib.{ Pagination, PaginatedCollection }
import java.util.UUID

import play.api._
import play.api.mvc._

object Application extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgsPage: Int = 0, membershipRequestsPage: Int = 0, publicOrgsPage: Int = 0) = Anonymous.async { implicit request =>
    request.user match {
      case None => {
        for {
          publicOrgs <- request.api.Organizations.get(
            limit = Some(Pagination.DefaultLimit+1),
            offset = Some(publicOrgsPage * Pagination.DefaultLimit)
          )
        } yield {
          Ok(
            views.html.index(
              request.mainTemplate("Organizations"),
              PaginatedCollection(orgsPage, Seq.empty),
              PaginatedCollection(membershipRequestsPage, Seq.empty),
              PaginatedCollection(publicOrgsPage, publicOrgs)
            )
          )
        }
      }

      case Some(user) => {
        for {
          orgs <- request.api.Organizations.get(
            userGuid = Some(user.guid),
            limit = Some(Pagination.DefaultLimit+1),
            offset = Some(orgsPage * Pagination.DefaultLimit)
          )
          membershipRequests <- request.api.MembershipRequests.get(
            userGuid = Some(user.guid),
            limit = Some(Pagination.DefaultLimit+1),
            offset = Some(membershipRequestsPage * Pagination.DefaultLimit)
          )
          publicOrgs <- request.api.Organizations.get(
            limit = Some(Pagination.DefaultLimit+1),
            offset = Some(publicOrgsPage * Pagination.DefaultLimit)
          )
        } yield {
          Ok(
            views.html.index(
              request.mainTemplate("Your Organizations"),
              PaginatedCollection(orgsPage, orgs),
              PaginatedCollection(membershipRequestsPage, membershipRequests),
              PaginatedCollection(publicOrgsPage, publicOrgs)
            )
          )
        }
      }
    }
  }

}
