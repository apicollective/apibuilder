package controllers

import db.Membership
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object Memberships extends Controller {

  def get(organization_guid: Option[UUID], organization_key: Option[String], user_guid: Option[UUID], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val memberships = Membership.findAll(organization_guid = organization_guid.map(_.toString),
                                         organization_key = organization_key,
                                         user_guid = user_guid.map(_.toString),
                                         role = role,
                                         limit = limit,
                                         offset = offset)
    Ok(Json.toJson(memberships))
  }

}
