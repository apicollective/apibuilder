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

}
