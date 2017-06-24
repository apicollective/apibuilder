package controllers

import io.apibuilder.apidoc.api.v0.models.Membership
import io.apibuilder.apidoc.api.v0.models.json._
import db.{Authorization, MembershipsDao}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.Json

@Singleton
class Memberships @Inject() (
  membershipsDao: MembershipsDao
) extends Controller {

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
        membershipsDao.findAll(
          request.authorization,
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
    membershipsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(membership) => {
        if (membershipsDao.isUserAdmin(request.user, membership.organization)) {
          Ok(Json.toJson(membership))
        } else {
          Unauthorized
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    membershipsDao.findByGuid(request.authorization, guid) match {
      case None => NoContent
      case Some(membership) => {
        if (membershipsDao.isUserAdmin(request.user, membership.organization)) {
          membershipsDao.softDelete(request.user, membership)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
