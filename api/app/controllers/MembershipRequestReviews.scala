package controllers

import core.Review
import lib.Validation
import db.MembershipRequest
import play.api.mvc._
import play.api.libs.json.Json

object MembershipRequestReviews extends Controller {

  def post() = Authenticated(parse.json) { request =>
    val action = (request.body \ "action").asOpt[String].getOrElse {
      sys.error("Missing action")
    }
    val review = Review.fromString(action).getOrElse {
      sys.error(s"Invalid action[${action}]")
    }
    val membershipRequestGuid = (request.body \ "membership_request_guid").asOpt[String].getOrElse {
      sys.error("Missing membership_request_guid")
    }
    println(s"review[${review}]")
    println(s"membershipRequestGuid[${membershipRequestGuid}]")

    MembershipRequest.findAll(guid = Some(membershipRequestGuid), limit = 1).headOption match {
      case None => {
        BadRequest(Json.toJson(Validation.error(s"Membership request not found or you are not authorized to access it")))
      }

      case Some(r: MembershipRequest) => {
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
