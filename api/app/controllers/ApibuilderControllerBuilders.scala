package controllers

import javax.inject.Inject

import io.apibuilder.api.v0.models.User
import lib.AppConfig
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ApibuilderControllerBuilders {
  def anonymousActionBuilder: AnonymousActionBuilder
  def identifiedActionBuilder: IdentifiedActionBuilder
}

case class AnonymousRequest[A](
  user: Option[User],
  request: Request[A]
)

case class IdentifiedRequest[A](
  user: User,
  request: Request[A]
)

class AnonymousActionBuilder @Inject()(
  val parser: BodyParsers.Default,
  val appConfig: AppConfig
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AnonymousRequest, AnyContent] {

  def invokeBlock[A](
    request: Request[A],
    block: (AnonymousRequest[A]
  ) => Future[Result]): Future[Result] = {
    val user: Option[User] = None
    block(AnonymousRequest(user, request))
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
