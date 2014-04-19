package controllers

import core.Review
import play.api._
import play.api.mvc._

object MembershipRequestReviews extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def accept(orgKey: String, membershipRequestGuid: String) = Authenticated.async { implicit request =>
    for {
      review <- request.client.membershipRequestReviews.post(membershipRequestGuid, Review.Accept.key)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request accepted")
    }
  }

  def decline(orgKey: String, membershipRequestGuid: String) = Authenticated.async { implicit request =>
    for {
      review <- request.client.membershipRequestReviews.post(membershipRequestGuid, Review.Decline.key)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request declined")
    }
  }

}
