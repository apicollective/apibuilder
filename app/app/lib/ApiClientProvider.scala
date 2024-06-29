package lib

import io.apibuilder.api.v0.models.User
import io.apibuilder.api.v0.{Authorization, Client}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import java.net.URLEncoder
import javax.inject.Inject

import play.api.libs.ws.WSClient

class ApiClientProvider @Inject() (
  wSClient: WSClient,
  config: Config
) {

  private val baseUrl = config.requiredString("apibuilder.api.host")
  private val apiAuth = Authorization.Basic(config.requiredString("apibuilder.api.token"))

  private def newClient(sessionId: Option[String]): Client = {
    new io.apibuilder.api.v0.Client(
      wSClient,
      baseUrl = baseUrl,
      auth = if (sessionId.isEmpty) Some(apiAuth) else None,
      defaultHeaders = sessionId.map { id =>
        "Authorization" -> ("Session " + URLEncoder.encode(id, "UTF-8"))
      }.toSeq
    )
  }

  private val unauthenticatedClient = newClient(None)

  def clientForSessionId(sessionId: Option[String]): Client  = {
    sessionId match {
      case None => unauthenticatedClient
      case Some(sid) => clientForSessionId(sid)
    }
  }

  def clientForSessionId(sessionId: String): Client = {
    newClient(Some(sessionId))
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

  private[lib] def awaitCallWith404[T](
    future: Future[T]
  )(implicit ec: ExecutionContext): Option[T] = {
    Await.result(
      callWith404(future),
      1000.millis
    )
  }

  private[lib] def await[T](
    future: Future[T]
  )(implicit ec: ExecutionContext): T = {
    Await.result(
      future,
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
