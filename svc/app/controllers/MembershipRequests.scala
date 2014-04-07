package controllers

import db.{ MembershipRequest, MembershipRequestJson }
import play.api.mvc._
import play.api.libs.json.Json

object MembershipRequests extends Controller {

  def get(organization_guid: Option[String], user_guid: Option[String], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val requests = MembershipRequest.findAll(user = Some(request.user),
                                             organization_guid = organization_guid,
                                             user_guid = user_guid,
                                             role = role,
                                             limit = limit,
                                             offset = offset)
    Ok(Json.toJson(requests.map(_.json)))
  }

  def post() = TODO

  def putGuidApprove(guid: String) = Authenticated { request =>
    MembershipRequest.findAll(guid = Some(guid), can_be_reviewed_by = Some(request.user), limit = 1).headOption match {
      case None => {
        NotFound
      }

      case Some(r: MembershipRequest) => {
        r.approve(request.user)
        Ok
      }
    }
  }

  def putGuidDecline(guid: String) = Authenticated { request =>
    MembershipRequest.findAll(guid = Some(guid), can_be_reviewed_by = Some(request.user), limit = 1).headOption match {
      case None => {
        NotFound
      }

      case Some(r: MembershipRequest) => {
        r.decline(request.user)
        Ok
      }
    }
  }


}
