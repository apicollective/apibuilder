package controllers

import db.Membership
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object Memberships extends Controller {

  def get(organizationGuid: Option[UUID], organizationKey: Option[String], userGuid: Option[UUID], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val memberships = Membership.findAll(organizationGuid = organizationGuid,
                                         organizationKey = organizationKey,
                                         userGuid = userGuid,
                                         role = role,
                                         limit = limit,
                                         offset = offset)
    Ok(Json.toJson(memberships))
  }

  def getByGuid(guid: java.util.UUID) = Authenticated { request =>
    Membership.findAll(guid = Some(guid.toString), limit = 1).headOption match {
      case None => NotFound
      case Some(membership) => {
        if (Membership.isUserAdmin(request.user, membership.organization)) {
          Ok(Json.toJson(membership))
        } else {
          Unauthorized
        }
      }
    }
  }

  def deleteByGuid(guid: java.util.UUID) = Authenticated { request =>
    val membership = Membership.findAll(guid = Some(guid.toString), limit = 1).headOption

    if (membership.isEmpty || Membership.isUserAdmin(request.user, membership.get.organization)) {
      membership.map { m => Membership.softDelete(request.user, m) }
      NoContent
    } else {
      Unauthorized
    }
  }

}
