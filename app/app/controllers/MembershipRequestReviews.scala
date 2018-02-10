package controllers

import java.util.UUID

import play.api._
import play.api.mvc._

class MembershipRequestReviews extends Controller {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def accept(orgKey: String, membershipRequestGuid: UUID) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      review <- request.api.MembershipRequests.postAcceptByGuid(membershipRequestGuid)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request accepted")
    }
  }

  def decline(orgKey: String, membershipRequestGuid: UUID) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      review <- request.api.MembershipRequests.postDeclineByGuid(membershipRequestGuid)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request declined")
    }
  }

}
