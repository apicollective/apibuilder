package controllers

import core.Review
import lib.Validation
import db.MembershipRequest
import play.api.mvc._
import play.api.libs.json.Json

object MembershipRequestReviews extends Controller {

  def post() = Authenticated(parse.json) { request =>
    println("ASFD")
    val action = (request.body \ "action").asOpt[String].getOrElse {
      sys.error("Missing action")
    }
    println("ACTION: " + action)

    val review = Review.fromString(action).getOrElse {
      sys.error(s"Invalid action[${action}]")
    }
    println("review: " + review)

    val membershipRequestGuid = (request.body \ "membership_request_guid").asOpt[String].getOrElse {
      sys.error("Missing membership_request_guid")
    }
    println("membershipRequestGuid: " + membershipRequestGuid)

    MembershipRequest.findAll(guid = Some(membershipRequestGuid), canBeReviewedByGuid = Some(request.user.guid), limit = 1).headOption match {
      case None => {
        println("not found")
        BadRequest(Json.toJson(Validation.error(s"Membership request not found or you are not authorized to access it")))
      }

      case Some(r: MembershipRequest) => {
        println("found r: " + r)
        review match {
          case Review.Accept => {
            r.accept(request.user)
          }
          case Review.Decline => {
            r.decline(request.user)
          }
          case other: Review => {
            sys.error(s"Unsupported review action[${other}]")
          }
        }
        Ok
      }
    }
  }

}
