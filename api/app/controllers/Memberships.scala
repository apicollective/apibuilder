package controllers

import com.gilt.apidoc.models.Membership
import com.gilt.apidoc.models.json._
import db.{Authorization, MembershipsDao}
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object Memberships extends Controller {

  def get(
    organizationGuid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[UUID],
    role: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    Ok(
      Json.toJson(
        MembershipsDao.findAll(
          Authorization(Some(request.user)),
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
    MembershipsDao.findByGuid(Authorization(Some(request.user)), guid) match {
      case None => NotFound
      case Some(membership) => {
        if (MembershipsDao.isUserAdmin(request.user, membership.organization)) {
          Ok(Json.toJson(membership))
        } else {
          Unauthorized
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    MembershipsDao.findByGuid(Authorization(Some(request.user)), guid) match {
      case None => NoContent
      case Some(membership) => {
        if (MembershipsDao.isUserAdmin(request.user, membership.organization)) {
          MembershipsDao.softDelete(request.user, membership)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
