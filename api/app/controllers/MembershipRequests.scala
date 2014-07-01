package controllers

import core.{ Review, Role }
import lib.Validation
import db.{ MembershipRequest, Organization, OrganizationDao, User, UserDao }
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object MembershipRequests extends Controller {

  case class MembershipRequestForm(
    org_guid: java.util.UUID,
    user_guid: java.util.UUID,
    role: String
  )

  object MembershipRequestForm {

      implicit val membershipRequestFormReads = Json.reads[MembershipRequestForm]

  }


  def get(organization_guid: Option[UUID], organization_key: Option[String], user_guid: Option[UUID], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val requests = MembershipRequest.findAll(organizationGuid = organization_guid.map(_.toString),
                                             organizationKey = organization_key,
                                             userGuid = user_guid.map(_.toString),
                                             role = role,
                                             limit = limit,
                                             offset = offset)
    Ok(Json.toJson(requests))
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[MembershipRequestForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error(e.toString)))
      }
      case s: JsSuccess[MembershipRequestForm] => {
        val form = s.get
        OrganizationDao.findByGuid(form.org_guid) match {
          case None => {
            Conflict(Json.toJson(Validation.error("Organization not found")))
          }

          case Some(org: Organization) => {
            UserDao.findByGuid(form.user_guid) match {

              case None => {
                Conflict(Json.toJson(Validation.error("User not found")))
              }

              case Some(user: User) => {
                Role.fromString(form.role) match {
                  case None => {
                    Conflict(Json.toJson(Validation.error("Invalid role")))
                  }

                  case Some(role: Role) => {
                    val mr = MembershipRequest.upsert(request.user, org, user, role)
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
    MembershipRequest.findAll(guid = Some(guid.toString), limit = 1).headOption match {
      case None => NotFound
      case Some(request: MembershipRequest) => {
        request.accept(request.user)
        NoContent
      }
    }
  }

  def postDeclineByGuid(guid: UUID) = Authenticated { request =>
    MembershipRequest.findAll(guid = Some(guid.toString), limit = 1).headOption match {
      case None => NotFound
      case Some(request: MembershipRequest) => {
        request.decline(request.user)
        NoContent
      }
    }
  }

}
