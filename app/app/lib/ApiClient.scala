package lib

import io.apibuilder.api.v0.models.User
import io.apibuilder.api.v0.{Authorization, Client}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import java.net.URLEncoder
import javax.inject.Inject

class ApiClientProvider @Inject() (
  config: Config
) {

  private[this] val unauthenticatedClient = ApiClient(config, None).client

  def clientForSessionId(sessionId: Option[String]) = {
    sessionId match {
      case None => unauthenticatedClient
      case Some(sid) => ApiClient(config, Some(sid)).client
    }
  }

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

case class ApiClient(
  config: Config,
  sessionId: Option[String]
) {

  private[this] val baseUrl = config.requiredString("apibuilder.api.host")
  private[this] val apiAuth = Authorization.Basic(config.requiredString("apibuilder.api.token"))

  val client: Client = new io.apibuilder.api.v0.Client(
    baseUrl = baseUrl,
    auth = Some(apiAuth),
    defaultHeaders = sessionId.map { id =>
      "Authorization" -> ("Session " + URLEncoder.encode(id, "UTF-8"))
    }.toSeq
  )

}
