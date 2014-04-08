package controllers

import db.{ MembershipRequest, MembershipRequestJson, Organization, OrganizationDao, User, UserDao }
import play.api.mvc._
import play.api.libs.json.Json
import java.util.UUID

object MembershipRequests extends Controller {

  def get(organization_guid: Option[String], user_guid: Option[String], role: Option[String], limit: Int = 50, offset: Int = 0) = Authenticated { request =>
    val requests = MembershipRequest.findAll(user = Some(request.user),
                                             organization_guid = organization_guid,
                                             user_guid = user_guid,
                                             role = role,
                                             limit = limit,
                                             offset = offset)
    Ok(Json.toJson(requests.map(_.json)))
  }

  def post() = Authenticated(parse.json) { request =>
    val role = (request.body \ "role").as[String]
    val organizationGuid = (request.body \ "organization_guid").as[String]
    val userGuid = (request.body \ "user_guid").as[String]

    OrganizationDao.findByUserAndGuid(request.user, UUID.fromString(organizationGuid)) match {
      case None => NotFound("Organization not found or you are not authorized to create membership requests for this org")
      case Some(org: Organization) => {
        UserDao.findByGuid(userGuid) match {
          case None => NotFound("User not found")
          case Some(user: User) => {
            val mr = MembershipRequest.upsert(request.user, org, user, role)
            Created(Json.toJson(mr.json))
          }
        }
      }
    }
  }

}
