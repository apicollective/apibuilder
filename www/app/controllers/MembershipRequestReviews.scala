package controllers

import java.util.UUID

import play.api._
import play.api.mvc._

object MembershipRequestReviews extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

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
