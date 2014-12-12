package controllers

import com.gilt.apidoc.models.Membership
import com.gilt.apidoc.models.json._
import db.Membership
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object Memberships extends Controller {

  def get(
    organizationGuid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[UUID],
    role: Option[String],
    limit: Long = 50,
    offset: Long = 0
  ) = Authenticated { request =>
    Ok(
      Json.toJson(
        db.Membership.findAll(
          organizationGuid = organizationGuid,
          organizationKey = organizationKey,
          userGuid = userGuid,
          role = role,
          limit = limit,
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Authenticated { request =>
    db.Membership.findByGuid(guid) match {
      case None => NotFound
      case Some(membership) => {
        if (db.Membership.isUserAdmin(request.user, membership.organization)) {
          Ok(Json.toJson(membership))
        } else {
          Unauthorized
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    db.Membership.findByGuid(guid) match {
      case None => NoContent
      case Some(membership) => {
        if (db.Membership.isUserAdmin(request.user, membership.organization)) {
          db.Membership.softDelete(request.user, membership)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
