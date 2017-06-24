package lib

import io.apibuilder.api.v0.models.User
import io.apibuilder.api.v0.{Authorization, Client}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import java.util.UUID
import java.net.URLEncoder

object ApiClient {

  private[this] val unauthenticatedClient = ApiClient(None).client

  def callWith404[T](
    f: Future[T]
  )(implicit ec: ExecutionContext): Future[Option[T]] = {
    f.map {
      value => Some(value)
    }.recover {
      case io.apibuilder.api.v0.errors.UnitResponse(404) => None
      case ex: Throwable => throw ex
    }
  }

  def awaitCallWith404[T](
    future: Future[T]
  )(implicit ec: ExecutionContext): Option[T] = {
    Await.result(
      callWith404(future),
      1000.millis
    )
  }

  /**
    * Blocking call to fetch a user. If the provided session id is not
    * valid, returns none.
    */
  def getUserBySessionId(
    sessionId: String
  )(implicit ec: ExecutionContext): Option[User] = {
    awaitCallWith404( unauthenticatedClient.authentications.getSessionById(sessionId) ).map(_.user)
  }

}

case class ApiClient(sessionId: Option[String]) {

  private[this] val baseUrl = Config.requiredString("apibuilder.api.host")
  private[this] val apiAuth = Authorization.Basic(Config.requiredString("apidoc.api.token"))

  val client: Client = new io.apibuilder.api.v0.Client(
    baseUrl = baseUrl,
    auth = Some(apiAuth),
    defaultHeaders = sessionId.map { id =>
      "Authorization" -> ("Session " + URLEncoder.encode(id, "UTF-8"))
    }.toSeq
  )

}
