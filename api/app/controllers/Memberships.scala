package controllers

import db.MembershipsDao
import io.apibuilder.api.v0.models.json._
import io.apibuilder.common.v0.models.MembershipRole
import play.api.libs.json.Json

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Memberships @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  membershipsDao: MembershipsDao
) extends ApiBuilderController {

  def get(
           organizationGuid: Option[UUID],
           organizationKey: Option[String],
           userGuid: Option[UUID],
           role: Option[MembershipRole],
           limit: Long = 25,
           offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        membershipsDao.findAll(
          request.authorization,
          organizationGuid = organizationGuid,
          organizationKey = organizationKey,
          userGuid = userGuid,
          role = role,
          limit = Some(limit),
          offset = offset
        )
      )
    )
  }

  def getByGuid(guid: UUID) = Identified { request =>
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

  def deleteByGuid(guid: UUID) = Identified { request =>
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
