package controllers

import com.bryzek.apidoc.api.v0.models.{ Membership, Organization, User, Visibility }
import models.MainTemplate
import lib.{ApiClient, Role}
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current

class AnonymousRequest[A](
  resources: RequestResources,
  val request: Request[A]
) extends WrappedRequest[A](request) {

  val user = resources.user
  val org = resources.org
  val isAdmin = resources.isAdmin
  val isMember = resources.isMember

  lazy val api = Authenticated.api(user)

  def mainTemplate(title: Option[String] = None): MainTemplate = {
    MainTemplate(
      requestPath = request.path,
      title = title,
      user = user,
      org = org,
      isOrgMember = isMember,
      isOrgAdmin = isAdmin
    )
  }

  def requireAdmin() {
    resources.requireAdmin()
  }

  def requireOrg() {
    resources.requireOrg()
  }
}

class AnonymousOrgRequest[A](
  anon: AnonymousRequest[A]
) extends WrappedRequest[A](anon.request) {

  val user = anon.user
  val org = anon.org.get
  val isAdmin = anon.isAdmin
  val isMember = anon.isMember

  lazy val api = anon.api

  def mainTemplate(title: Option[String] = None): MainTemplate = anon.mainTemplate(title = title)

  def requireAdmin() {
    anon.requireAdmin()
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

  /**
    * Blocking call to fetch an organization
    */
  private[this] def getOrganization(user: Option[User], key: String): Option[Organization] = {
    ApiClient.awaitCallWith404( Authenticated.api(user).Organizations.getByKey(key) )
  }

  def resources(requestPath: String, sessionId: Option[String]): RequestResources = {
    val user = sessionId.flatMap { ApiClient.getUserBySessionId(_) }
    val org = requestPath.split("/").drop(1).headOption.flatMap { getOrganization(user, _) }

    val memberships = (user, org) match {
      case (Some(u), Some(o)) => {
        Await.result(Authenticated.api(user).Memberships.get(orgKey = Some(o.key), userGuid = Some(u.guid)), 1000.millis)
      }
      case _ => Seq.empty
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
    val resources = AnonymousRequest.resources(request.path, request.session.get("session_id"))
    block(new AnonymousRequest(resources, request))
  }
}

object AnonymousOrg extends ActionBuilder[AnonymousOrgRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AnonymousOrgRequest[A]) => Future[Result]) = {
    val resources = AnonymousRequest.resources(request.path, request.session.get("session_id"))
    if (resources.org.isEmpty) {
      Future.successful(Redirect("/").flashing("warning" -> "Org not found or access denied"))
    } else {
      val anon = new AnonymousRequest(resources, request)
      val anonRequest = new AnonymousOrgRequest(anon)

      if (resources.isMember) {
        block(anonRequest)
      } else if (resources.org.map(_.visibility) == Some(Visibility.Public)) {
        block(anonRequest)
      } else {
        Future.successful(Redirect("/").flashing("warning" -> "Org not found or access denied"))
      }
    }
  }
}
