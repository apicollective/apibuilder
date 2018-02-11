package controllers

import javax.inject.Inject

import com.google.inject.ImplementedBy
import io.apibuilder.api.v0.models.{Membership, Organization, User}
import lib.{ApibuilderRequestData, RequestAuthenticationUtil}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Main trait for controllers to implement.
  * Extends the play base controller to wire up authentication to include:
  *   - anonymous / api key / session based access
  *   - utilities to check for organization role
  */
trait ApibuilderController extends BaseController with I18nSupport {

  protected def apibuilderControllerComponents: ApibuilderControllerComponents

  def Anonymous: AnonymousActionBuilder = apibuilderControllerComponents.anonymousActionBuilder
  def AnonymousOrg: AnonymousOrgActionBuilder = apibuilderControllerComponents.anonymousOrgActionBuilder
  def Identified: IdentifiedActionBuilder = apibuilderControllerComponents.identifiedActionBuilder
  def IdentifiedOrg: IdentifiedOrgActionBuilder = apibuilderControllerComponents.identifiedOrgActionBuilder

  def controllerComponents: ControllerComponents = apibuilderControllerComponents.controllerComponents
  override def messagesApi: MessagesApi = apibuilderControllerComponents.messagesApi
}

@ImplementedBy(classOf[ApibuilderDefaultControllerComponents])
trait ApibuilderControllerComponents {
  def anonymousActionBuilder: AnonymousActionBuilder
  def anonymousOrgActionBuilder: AnonymousOrgActionBuilder
  def identifiedActionBuilder: IdentifiedActionBuilder
  def identifiedOrgActionBuilder: IdentifiedOrgActionBuilder
  def controllerComponents: ControllerComponents
  def messagesApi: MessagesApi
}

class ApibuilderDefaultControllerComponents @Inject() (
  val controllerComponents: ControllerComponents,
  val anonymousActionBuilder: AnonymousActionBuilder,
  val anonymousOrgActionBuilder: AnonymousOrgActionBuilder,
  val identifiedActionBuilder: IdentifiedActionBuilder,
  val identifiedOrgActionBuilder: IdentifiedOrgActionBuilder,
  val messagesApi: MessagesApi
) extends ApibuilderControllerComponents

case class AnonymousRequest[A](
  requestData: ApibuilderRequestData,
  request: Request[A]
) extends WrappedRequest(request) {
  val user: Option[User] = requestData.user
  val org: Option[Organization] = requestData.org
  val memberships: Seq[Membership] = requestData.memberships
  val api = requestData.api

  def mainTemplate(title: Option[String] = None) = requestData.mainTemplate(title)
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
    val data = requestAuthenticationUtil.data(
      requestPath = request.path,
      sessionId = request.session.get("session_id")
    )

    block(AnonymousRequest(data, request))
  }
}

case class IdentifiedRequest[A](
  requestData: ApibuilderRequestData,
  request: Request[A]
) extends WrappedRequest(request) {
  val user: User = requestData.user.getOrElse {
    sys.error("Identified request must have a user")
  }
  val org: Option[Organization] = requestData.org
  val memberships: Seq[Membership] = requestData.memberships
  val api = requestData.api

  def mainTemplate(title: Option[String] = None) = requestData.mainTemplate(title)
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
    val data = requestAuthenticationUtil.data(
      requestPath = request.path,
      sessionId = request.session.get("session_id")
    )

    data.user match {
      case None => sys.error("TODO: Redirect to login")
      case Some(_) => block(IdentifiedRequest(data, request))
    }
  }
}

case class AnonymousOrgRequest[A](
  requestData: ApibuilderRequestData,
  request: Request[A]
) extends WrappedRequest(request) {
  val user: Option[User] = requestData.user
  val org: Organization = requestData.org.getOrElse {
    sys.error("IdentifiedOrg request must have an organization")
  }
  val memberships: Seq[Membership] = requestData.memberships
  val api = requestData.api

  def mainTemplate(title: Option[String] = None) = requestData.mainTemplate(title)
}

class AnonymousOrgActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  requestAuthenticationUtil: RequestAuthenticationUtil
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AnonymousOrgRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (AnonymousOrgRequest[A]
      ) => Future[Result]): Future[Result] = {
    val data = requestAuthenticationUtil.data(
      requestPath = request.path,
      sessionId = request.session.get("session_id")
    )

    data.org match {
      case None => {
        sys.error("TODO: Redirect to home page - invalid request")
      }
      case Some(_) => {
        block(AnonymousOrgRequest(data, request))
      }
    }
  }
}


case class IdentifiedOrgRequest[A](
  requestData: ApibuilderRequestData,
  request: Request[A]
) extends WrappedRequest(request) {
  val user: User = requestData.user.getOrElse {
    sys.error("Identified request must have a user")
  }
  val org: Organization = requestData.org.getOrElse {
    sys.error("IdentifiedOrg request must have an organization")
  }
  val memberships: Seq[Membership] = requestData.memberships
  val api = requestData.api

  def mainTemplate(title: Option[String] = None) = requestData.mainTemplate(title)
}

class IdentifiedOrgActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  requestAuthenticationUtil: RequestAuthenticationUtil
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[IdentifiedOrgRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (IdentifiedOrgRequest[A]
      ) => Future[Result]): Future[Result] = {
    val data = requestAuthenticationUtil.data(
      requestPath = request.path,
      sessionId = request.session.get("session_id")
    )

    data.user match {
      case None => sys.error("TODO: Redirect to login")
      case Some(_) => {
        data.org match {
          case None => {
            sys.error("TODO: Redirect to home page - invalid request")
          }
          case Some(_) => {
            block(IdentifiedOrgRequest(data, request))
          }
        }
      }
    }
  }
}
