package controllers

import db.{ Membership, MembershipJson }
import play.api.mvc._
import play.api.libs.json.Json

object Memberships extends Controller {

  def get(organization_guid: Option[String], user_guid: Option[String], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val memberships = Membership.findAll(user = Some(request.user),
                                         organization_guid = organization_guid,
                                         user_guid = user_guid,
                                         role = role,
                                         limit = limit,
                                         offset = offset)
    Ok(Json.toJson(memberships.map(_.json)))
  }

}
