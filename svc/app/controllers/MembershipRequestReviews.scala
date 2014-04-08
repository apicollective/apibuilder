package controllers

import db.MembershipRequest
import play.api.mvc._
import play.api.libs.json.Json

object MembershipRequestReviews extends Controller {

  def post() = Authenticated(parse.json) { request =>
    val action = (request.body \ "action").asOpt[String].getOrElse {
      sys.error("Missing action")
    }
    val membershipRequestGuid = (request.body \ "membership_request_guid").asOpt[String].getOrElse {
      sys.error("Missing membership_request_guid")
    }

    MembershipRequest.findAll(guid = Some(membershipRequestGuid), can_be_reviewed_by = Some(request.user), limit = 1).headOption match {
      case None => {
        NotFound
      }

      case Some(r: MembershipRequest) => {
        action match {
          case "approve" => {
            r.approve(request.user)
          }
          case "decline" => {
            r.decline(request.user)
          }
          case other: String => {
            sys.error(s"Invalid action[${other}]")
          }
        }
        Ok
      }
    }
  }

}
