package controllers

import javax.inject.Inject

import com.google.inject.ImplementedBy
import db.{Authorization, MembershipsDao, OrganizationsDao}
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.api.v0.models.json._
import lib.{AppConfig, Role, Validation}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ApibuilderController extends BaseController {

  protected def apibuilderControllerComponents: ApibuilderControllerComponents

  def Anonymous: AnonymousActionBuilder = apibuilderControllerComponents.anonymousActionBuilder
  def Identified: IdentifiedActionBuilder = apibuilderControllerComponents.identifiedActionBuilder

  def controllerComponents: ControllerComponents = apibuilderControllerComponents.controllerComponents

  def membershipsDao: MembershipsDao = apibuilderControllerComponents.membershipsDao
  def organizationsDao: OrganizationsDao = apibuilderControllerComponents.organizationsDao

  def withOrg(auth: Authorization, orgKey: String)(f: Organization => Result) = {
    organizationsDao.findByKey(auth, orgKey) match {
      case None => Results.NotFound(
        Json.toJson(
          Validation.error(
            s"Organization[$orgKey] does not exist or you are not authorized to access it"
          )
        )
      )

      case Some(org) => {
        f(org)
      }
    }
  }

  def withOrgMember(user: User, orgKey: String)(f: Organization => Result) = {
    println(s"withOrgMember(${user.guid}, $orgKey)")
    withOrg(Authorization.User(user.guid), orgKey) { org =>
      println(s"org[${org.guid}] membership check")
      withRole(org, user, Role.All) {
        println("is a member")
        f(org)
      }
    }
  }

  def withOrgAdmin(user: User, orgKey: String)(f: Organization => Result) = {
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
        Json.toJson(
          Validation.error(
            s"Must be $msg of the organization"
          )
        )
      )
    } else {
      f
    }
  }
}

@ImplementedBy(classOf[ApibuilderDefaultControllerComponents])
trait ApibuilderControllerComponents {
  def anonymousActionBuilder: AnonymousActionBuilder
  def identifiedActionBuilder: IdentifiedActionBuilder
  def controllerComponents: ControllerComponents
  def membershipsDao: MembershipsDao
  def organizationsDao: OrganizationsDao
}

class ApibuilderDefaultControllerComponents @Inject() (
  val controllerComponents: ControllerComponents,
  val anonymousActionBuilder: AnonymousActionBuilder,
  val identifiedActionBuilder: IdentifiedActionBuilder,
  val membershipsDao: MembershipsDao,
  val organizationsDao: OrganizationsDao
) extends ApibuilderControllerComponents

case class Anonymous[A](
  user: Option[User],
  request: Request[A]
) extends WrappedRequest(request) {
  val authorization = user match {
    case None => Authorization.PublicOnly
    case Some(u) => Authorization.User(u.guid)
  }
}

case class IdentifiedRequest[A](
  user: User,
  request: Request[A]
) extends WrappedRequest(request) {
  val authorization = Authorization.User(user.guid)
}

class AnonymousActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  val appConfig: AppConfig
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[Anonymous, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (Anonymous[A]
  ) => Future[Result]): Future[Result] = {
    val user: Option[User] = None
    block(Anonymous(user, request))
  }
}

class IdentifiedActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  val appConfig: AppConfig
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[IdentifiedRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (IdentifiedRequest[A]
  ) => Future[Result]): Future[Result] = {
    println(s"HEADERS: ${request.headers.toString}")

    Future.successful(Results.Unauthorized)
    //auth(request.headers)(AuthData.Identified.fromMap) match {
    //    case None => Future.successful(unauthorized(request))
    //   case Some(ad) => block(new IdentifiedRequest(ad, request))
    // }
  }
}
