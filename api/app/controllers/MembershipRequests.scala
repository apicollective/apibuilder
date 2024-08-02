package controllers

import db.{InternalMembershipRequestsDao, InternalOrganizationsDao, InternalUsersDao}
import io.apibuilder.api.v0.models.json._
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.common.v0.models.MembershipRole
import lib.Validation
import models.MembershipRequestsModel
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class MembershipRequests @Inject() (
                                     val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                     membershipRequestsDao: InternalMembershipRequestsDao,
                                     organizationsDao: InternalOrganizationsDao,
                                     usersDao: InternalUsersDao,
                                     model: MembershipRequestsModel
) extends ApiBuilderController {

  case class MembershipRequestForm(
    org_guid: java.util.UUID,
    user_guid: java.util.UUID,
    role: String
  )

  object MembershipRequestForm {

      private[controllers] implicit val membershipRequestFormReads: Reads[MembershipRequestForm] = Json.reads[MembershipRequestForm]

  }

  def get(
    organizationGuid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[UUID],
    role: Option[MembershipRole],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    val requests = membershipRequestsDao.findAll(
      request.authorization,
      organizationGuid = organizationGuid,
      organizationKey = organizationKey,
      userGuid = userGuid,
      role = role,
      limit = Some(limit),
      offset = offset
    )
    Ok(Json.toJson(model.toModels(requests)))
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[MembershipRequestForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error(e.toString)))
      }
      case JsSuccess(form: MembershipRequestForm, _) => {
        organizationsDao.findByGuid(request.authorization, form.org_guid) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Organization not found or not authorized to make changes to this org")))
          }

          case Some(org) => {
            usersDao.findByGuid(form.user_guid) match {

              case None => {
                Conflict(Json.toJson(Validation.error("User not found")))
              }

              case Some(user) => {
                MembershipRole.fromString(form.role) match {
                  case None => {
                    Conflict(Json.toJson(Validation.error("Invalid role")))
                  }

                  case Some(role) => {
                    val mr = membershipRequestsDao.upsert(request.user, org, user, role)
                    Ok(Json.toJson(
                      model.toModel(mr).getOrElse {
                        sys.error("Failed to convert new membership request to model")
                      }
                    ))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def postAcceptByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    membershipRequestsDao.findByGuid(request.authorization, guid).flatMap(model.toModel) match {
      case None => NotFound
      case Some(mr) => {
        membershipRequestsDao.accept(request.user, mr)
        NoContent
      }
    }
  }

  def postDeclineByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    membershipRequestsDao.findByGuid(request.authorization, guid).flatMap(model.toModel) match {
      case None => NotFound
      case Some(mr) => {
        membershipRequestsDao.decline(request.user, mr)
        NoContent
      }
    }
  }

}
