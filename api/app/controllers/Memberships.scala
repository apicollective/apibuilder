package controllers

import db.{MembershipsDao, OrganizationReference}
import io.apibuilder.api.v0.models.json._
import io.apibuilder.common.v0.models.MembershipRole
import models.MembershipsModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Memberships @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  membershipsDao: MembershipsDao,
  model: MembershipsModel,
) extends ApiBuilderController {

  def get(
           organizationGuid: Option[UUID],
           organizationKey: Option[String],
           userGuid: Option[UUID],
           role: Option[MembershipRole],
           limit: Long = 25,
           offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    Ok(
      Json.toJson(
        model.toModels(
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
    )
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    membershipsDao.findByGuid(request.authorization, guid).flatMap(model.toModel) match {
      case None => NotFound
      case Some(membership) => {
        if (membershipsDao.isUserAdmin(user = request.user, organization = OrganizationReference(membership.organization))) {
          Ok(Json.toJson(membership))
        } else {
          Unauthorized
        }
      }
    }
  }

  def deleteByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    membershipsDao.findByGuid(request.authorization, guid) match {
      case None => NoContent
      case Some(membership) => {
        if (membershipsDao.isUserAdmin(userGuid = request.user.guid, organizationGuid = membership.organizationGuid)) {
          membershipsDao.softDelete(request.user, membership)
          NoContent
        } else {
          Unauthorized
        }
      }
    }
  }

}
