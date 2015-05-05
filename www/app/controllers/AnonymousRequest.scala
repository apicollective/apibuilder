package controllers

import com.gilt.apidoc.api.v0.models.{ Membership, Organization, User, Visibility }
import models.MainTemplate
import lib.Role
import play.api.mvc._
import play.api.mvc.Results.Redirect
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import play.api.Play.current
import java.util.UUID
import scala.util.{Failure, Success, Try}

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

  def callWith404[T](
    future: Future[T]
  ): Option[T] = {
    Await.ready(future, 1000.millis).value.get match {
      case Success(value) => Some(value)
      case Failure(ex) => ex match {
        case com.gilt.apidoc.api.v0.errors.UnitResponse(404) => None
        case _ => throw ex
      }
    }
  }
  
  /**
    * Blocking call to fetch a user. If the provided guid is not a
    * valid UUID, returns none.
    */
  def getUser(guid: String): Option[User] = {
    Try(UUID.fromString(guid)) match {
      case Success(userGuid) => callWith404( Authenticated.api().Users.getByGuid(userGuid) )
      case Failure(ex) => None
    }
  }

  /**
    * Blocking call to fetch an organization
    */
  def getOrganization(user: Option[User], key: String): Option[Organization] = {
    callWith404( Authenticated.api(user).Organizations.getByKey(key) )
  }

  def resources(requestPath: String, userGuid: Option[String]): RequestResources = {
    val user = userGuid.flatMap { getUser(_) }
    val org = requestPath.split("/").drop(1).headOption.flatMap { getOrganization(user, _) }

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

object AnonymousOrg extends ActionBuilder[AnonymousOrgRequest] {

  import scala.concurrent.ExecutionContext.Implicits.global

  def invokeBlock[A](request: Request[A], block: (AnonymousOrgRequest[A]) => Future[Result]) = {
    val resources = AnonymousRequest.resources(request.path, request.session.get("user_guid"))
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
