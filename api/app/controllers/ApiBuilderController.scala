package controllers

import javax.inject.Inject

import com.google.inject.ImplementedBy
import db.{Authorization, MembershipsDao, OrganizationsDao}
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.api.v0.models.json._
import lib.{RequestAuthenticationUtil, Role, Validation}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Main trait for controllers to implement.
  * Extends the play base controller to wire up authentication to include:
  *   - anonymous / api key / session based access
  *   - utilities to check for organization role
  */
trait ApiBuilderController extends BaseController {

  protected def apiBuilderControllerComponents: ApiBuilderControllerComponents

  def Anonymous: AnonymousActionBuilder = apiBuilderControllerComponents.anonymousActionBuilder
  def Identified: IdentifiedActionBuilder = apiBuilderControllerComponents.identifiedActionBuilder

  def controllerComponents: ControllerComponents = apiBuilderControllerComponents.controllerComponents

  def membershipsDao: MembershipsDao = apiBuilderControllerComponents.membershipsDao
  def organizationsDao: OrganizationsDao = apiBuilderControllerComponents.organizationsDao

  def withOrg(auth: Authorization, orgKey: String)(f: Organization => Result): Result = {
    organizationsDao.findByKey(auth, orgKey) match {
      case None => Results.NotFound(
        jsonError(s"Organization[$orgKey] does not exist or you are not authorized to access it")
      )

      case Some(org) => {
        f(org)
      }
    }
  }

  def withOrgMember(user: User, orgKey: String)(f: Organization => Result): Result = {
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      withRole(org, user, Role.All) {
        f(org)
      }
    }
  }

  def withOrgAdmin(user: User, orgKey: String)(f: Organization => Result): Result = {
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      withRole(org, user, Seq(Role.Admin)) {
        f(org)
      }
    }
  }

  private[this] def withRole(org: Organization, user: User, roles: Seq[Role])(f: => Result): Result = {
    val actualRoles = membershipsDao.findByOrganizationAndUserAndRoles(
      Authorization.All, org, user, roles
    ).map(_.role)

    if (actualRoles.isEmpty) {
      val msg: String = if (roles.contains(Role.Admin)) {
        s"an '${Role.Admin}'"
      } else {
        s"a '${Role.Member}'"
      }
      Results.Unauthorized(
        jsonError(s"Must be $msg of the organization")
      )
    } else {
      f
    }
  }

  private[this] def jsonError(message: String): JsValue = {
    Json.toJson(
      Validation.error(
        message
      )
    )
  }
}

@ImplementedBy(classOf[ApiBuilderDefaultControllerComponents])
trait ApiBuilderControllerComponents {
  def anonymousActionBuilder: AnonymousActionBuilder
  def identifiedActionBuilder: IdentifiedActionBuilder
  def controllerComponents: ControllerComponents
  def membershipsDao: MembershipsDao
  def organizationsDao: OrganizationsDao
}

class ApiBuilderDefaultControllerComponents @Inject() (
  val controllerComponents: ControllerComponents,
  val anonymousActionBuilder: AnonymousActionBuilder,
  val identifiedActionBuilder: IdentifiedActionBuilder,
  val membershipsDao: MembershipsDao,
  val organizationsDao: OrganizationsDao
) extends ApiBuilderControllerComponents

case class AnonymousRequest[A](
  user: Option[User],
  request: Request[A]
) extends WrappedRequest(request) {
  val authorization: Authorization = user match {
    case None => Authorization.PublicOnly
    case Some(u) => Authorization.User(u.guid)
  }
}

case class IdentifiedRequest[A](
  user: User,
  request: Request[A]
) extends WrappedRequest(request) {
  val authorization: Authorization.User = Authorization.User(user.guid)
}

class AnonymousActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  requestAuthenticationUtil: RequestAuthenticationUtil
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AnonymousRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (AnonymousRequest[A]
  ) => Future[Result]): Future[Result] = {
    block(
      AnonymousRequest(
        user = requestAuthenticationUtil.user(request.headers),
        request = request
      )
    )
  }
}

class IdentifiedActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  requestAuthenticationUtil: RequestAuthenticationUtil
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[IdentifiedRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (IdentifiedRequest[A]
  ) => Future[Result]): Future[Result] = {
    requestAuthenticationUtil.user(request.headers) match {
      case None => Future.successful(Results.Unauthorized)
      case Some(user) => block(IdentifiedRequest(user, request))
    }
  }
}
