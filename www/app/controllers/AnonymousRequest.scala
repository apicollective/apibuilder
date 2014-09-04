package controllers

import com.gilt.apidoc.models.{ Membership, Organization, User }
import models.MainTemplate
import core.Role
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID

class AnonymousRequest[A](
  resources: RequestResources,
  request: Request[A]
) extends WrappedRequest[A](request) {

  val user = resources.user
  val org = resources.org

  lazy val api = Authenticated.api(user)

  def mainTemplate(title: String): MainTemplate = {
    MainTemplate(
      title = title,
      user = resources.user,
      org = resources.org
    )
  }

  def requireAdmin() {
    resources.requireAdmin()
  }

  def requireOrg() {
    resources.requireOrg()
  }
}

case class RequestResources(
  user: Option[User],
  org: Option[Organization],
  memberships: Seq[Membership]
) {
  require(
    memberships.isEmpty || (!org.isEmpty || !user.isEmpty),
    "memberships must be empty if there is no organization or no user"
  )

  val isAdmin = !memberships.find(_.role == Role.Admin.key).isEmpty
  val isMember = isAdmin || !memberships.find(_.role == Role.Member.key).isEmpty

  def requireUser() {
    require(!user.isEmpty, "Action requires an authenticated user")
  }

  def requireOrg() {
    require(!org.isEmpty, "Action requires an org")
  }

  def requireAdmin() {
    requireUser()
    requireOrg()
    require(isAdmin, s"Action requires admin role. User[${user.get.guid}] is not an admin of Org[${org.get.key}]")
  }

  def requireMember() {
    requireUser()
    requireOrg()
    require(isMember, s"Action requires member role. User[${user.get.guid}] is not a member of Org[${org.get.key}]")
  }

}

object AnonymousRequest {

  import scala.concurrent.ExecutionContext.Implicits.global

  def resources(requestPath: String, userGuid: Option[String]): RequestResources = {
    val user = userGuid.flatMap { guid =>
      Await.result(Authenticated.api().Users.getByGuid(UUID.fromString(guid)), 5000.millis)
    }

    val org = requestPath.split("/").drop(1).headOption.flatMap { possibleOrgKey =>
      Await.result(Authenticated.api(user).Organizations.get(key = Some(possibleOrgKey)), 1000.millis).headOption
    }

    val memberships = if (user.isEmpty || org.isEmpty) {
      Seq.empty
    } else {
      Await.result(Authenticated.api(user).Memberships.get(orgKey = Some(org.get.key), userGuid = Some(user.get.guid)), 1000.millis)
    }

    RequestResources(
      user = user,
      org = org,
      memberships = memberships
    )
  }

}

object Anonymous extends ActionBuilder[AnonymousRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AnonymousRequest[A]) => Future[Result]) = {
    val resources = AnonymousRequest.resources(request.path, request.session.get("user_guid"))
    block(new AnonymousRequest(resources, request))
  }
}
