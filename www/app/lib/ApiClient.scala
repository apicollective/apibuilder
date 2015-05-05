package lib

import com.gilt.apidoc.api.v0.models.User
import com.gilt.apidoc.api.v0.{Authorization, Client}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import java.util.UUID

object ApiClient {

  private val unauthenticatedClient = ApiClient(None).client

  def callWith404[T](
    f: Future[T]
  ): Future[Option[T]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      awaitCallWith404(f)
    }
  }

  def awaitCallWith404[T](
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
  def getUser(guid: String)(implicit ec: ExecutionContext): Option[User] = {
    Try(UUID.fromString(guid)) match {
      case Success(userGuid) => awaitCallWith404( unauthenticatedClient.users.getByGuid(userGuid) )
      case Failure(ex) => None
    }
  }

}

case class ApiClient(user: Option[User]) {

  private val apiHost = Config.requiredString("apidoc.api.host")
  private val apiAuth = Authorization.Basic(Config.requiredString("apidoc.api.token"))
  private val defaultHeaders = Seq(
    user.map { u =>
      ("X-User-Guid", u.guid.toString)
    }
  ).flatten

  val client: Client = new com.gilt.apidoc.api.v0.Client(
    apiUrl = apiHost,
    auth = Some(apiAuth),
    defaultHeaders = defaultHeaders
  )

}
