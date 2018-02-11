package controllers

import javax.inject.Inject

import db.Authorization
import io.apibuilder.api.v0.models.User
import lib.AppConfig
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ApibuilderControllerBuilders {
  protected def apibuilderControllerComponents: ApibuilderControllerComponents

  def Anonymous: AnonymousActionBuilder = apibuilderControllerComponents.anonymousActionBuilder
  def Identified: IdentifiedActionBuilder = apibuilderControllerComponents.identifiedActionBuilder
}

trait ApibuilderControllerComponents {
  def anonymousActionBuilder: AnonymousActionBuilder
  def identifiedActionBuilder: IdentifiedActionBuilder
}

class ApibuilderDefaultControllerComponents @Inject() (
  val anonymousActionBuilder: AnonymousActionBuilder,
  val identifiedActionBuilder: IdentifiedActionBuilder
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
