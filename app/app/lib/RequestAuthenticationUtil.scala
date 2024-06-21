package lib

import javax.inject.Inject
import io.apibuilder.api.v0.Client
import io.apibuilder.api.v0.models.{Membership, Organization, User}
import io.apibuilder.common.v0.models.MembershipRole
import models.MainTemplate

import scala.concurrent.ExecutionContext

case class ApibuilderRequestData(
  api: Client,
  requestPath: String,
  sessionId: Option[String],
  user: Option[User],
  org: Option[Organization],
  memberships: Seq[Membership]
) {
  val isAdmin: Boolean = memberships.exists(_.role == MembershipRole.Admin)
  val isMember: Boolean = isAdmin || memberships.exists(_.role == MembershipRole.Member)

  def mainTemplate(title: Option[String] = None): MainTemplate = {
    MainTemplate(
      requestPath = requestPath,
      title = title,
      user = user,
      org = org,
      isOrgMember = isMember,
      isOrgAdmin = isAdmin
    )
  }
}

/**
* Helpers to fetch a user from an incoming request header
*/
class RequestAuthenticationUtil @Inject() (
  apiClientProvider: ApiClientProvider
) {

  private[this] def getMemberships(
    sessionId: Option[String],
    org: Organization,
    user: User
  ) (
    implicit ec: ExecutionContext
  ): Seq[Membership] = {
    apiClientProvider.await {
      apiClientProvider.clientForSessionId(sessionId).memberships.get(
        orgKey = Some(org.key),
        userGuid = Some(user.guid)
      )
    }
  }

  private[this] def getOrganization(
    sessionId: Option[String],
    key: String
  ) (
    implicit ec: ExecutionContext
  ): Option[Organization] = {
    apiClientProvider.awaitCallWith404 {
      apiClientProvider.clientForSessionId(sessionId).organizations.getByKey(key)
    }
  }

  def data(
    requestPath: String,
    sessionId: Option[String]
  ) (
    implicit ec: ExecutionContext
  ): ApibuilderRequestData = {
    val user = sessionId.flatMap { apiClientProvider.getUserBySessionId }
    val org = requestPath.split("/").drop(1).headOption.flatMap { orgKey =>
      getOrganization(sessionId, orgKey)
    }

    val memberships = (user, org) match {
      case (Some(u), Some(o)) => getMemberships(sessionId, o, u)
      case _ => Seq.empty
    }

    ApibuilderRequestData(
      api = apiClientProvider.clientForSessionId(sessionId),
      requestPath = requestPath,
      sessionId = sessionId,
      user = user,
      org = org,
      memberships = memberships
    )

  }

}
