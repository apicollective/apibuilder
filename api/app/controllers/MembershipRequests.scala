package controllers

import com.bryzek.apidoc.api.v0.models.{Organization, User}
import com.bryzek.apidoc.api.v0.models.json._
import lib.{Review, Role, Validation}
import db.{Authorization, MembershipRequestsDao, OrganizationsDao, UsersDao}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class MembershipRequests @Inject() (
  membershipRequestsDao: MembershipRequestsDao,
  organizationsDao: OrganizationsDao,
  usersDao: UsersDao
) {

  case class MembershipRequestForm(
    org_guid: java.util.UUID,
    user_guid: java.util.UUID,
    role: String
  )

  object MembershipRequestForm {

      implicit val membershipRequestFormReads = Json.reads[MembershipRequestForm]

  }

  def get(
    organizationGuid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[UUID],
    role: Option[String],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val requests = membershipRequestsDao.findAll(
      request.authorization,
      organizationGuid = organizationGuid,
      organizationKey = organizationKey,
      userGuid = userGuid,
      role = role,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(requests))
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[MembershipRequestForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error(e.toString)))
      }
      case s: JsSuccess[MembershipRequestForm] => {
        val form = s.get
        organizationsDao.findByUserAndGuid(request.user, form.org_guid) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Organization not found or not authorized to make changes to this org")))
          }

          case Some(org: Organization) => {
            usersDao.findByGuid(form.user_guid) match {

              case None => {
                Conflict(Json.toJson(Validation.error("User not found")))
              }

              case Some(user: User) => {
                Role.fromString(form.role) match {
                  case None => {
                    Conflict(Json.toJson(Validation.error("Invalid role")))
                  }

                  case Some(role: Role) => {
                    val mr = membershipRequestsDao.upsert(request.user, org, user, role)
                    Ok(Json.toJson(mr))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def postAcceptByGuid(guid: UUID) = Authenticated { request =>
    membershipRequestsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(mr) => {
        membershipRequestsDao.accept(request.user, mr)
        NoContent
      }
    }
  }

  def postDeclineByGuid(guid: UUID) = Authenticated { request =>
    membershipRequestsDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(mr) => {
        membershipRequestsDao.decline(request.user, mr)
        NoContent
      }
    }
  }

}
