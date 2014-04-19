package controllers

import core.Role
import lib.Validation
import db.{ MembershipRequest, MembershipRequestJson, Organization, OrganizationDao, User, UserDao }
import play.api.mvc._
import play.api.libs.json.Json

object MembershipRequests extends Controller {

  def get(organization_guid: Option[String], organization_key: Option[String], can_be_reviewed_by_guid: Option[String], user_guid: Option[String], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val requests = MembershipRequest.findAll(user = Some(request.user),
                                             organizationGuid = organization_guid,
                                             organizationKey = organization_key,
                                             canBeReviewedByGuid = can_be_reviewed_by_guid,
                                             userGuid = user_guid,
                                             role = role,
                                             limit = limit,
                                             offset = offset)
    Ok(Json.toJson(requests.map(_.json)))
  }

  def post() = Authenticated(parse.json) { request =>
    val roleKey = (request.body \ "role").as[String]
    val role = Role.fromString(roleKey).getOrElse {
      sys.error(s"Invalid role[$roleKey]")
    }
    val organizationGuid = (request.body \ "organization_guid").as[String]
    val userGuid = (request.body \ "user_guid").as[String]

    OrganizationDao.findAll(guid = Some(organizationGuid), limit = 1).headOption match {

      case None => {
        BadRequest(Json.toJson(Validation.error("Organization not found")))
      }

      case Some(org: Organization) => {
        UserDao.findByGuid(userGuid) match {

          case None => {
            BadRequest(Json.toJson(Validation.error("User not found")))
          }

          case Some(user: User) => {
            val mr = MembershipRequest.upsert(request.user, org, user, role)
            println(Json.toJson(mr.json))
            Created(Json.toJson(mr.json))
          }
        }
      }
    }
  }

}
