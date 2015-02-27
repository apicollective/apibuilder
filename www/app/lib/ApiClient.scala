package lib

import com.gilt.apidoc.v0.models.User
import com.gilt.apidoc.v0.{Authorization, Client}

case class ApiClient(user: Option[User]) {

  private val apiHost = Config.requiredString("apidoc.api.host")
  private lazy val apiAuth = Authorization.Basic(Config.requiredString("apidoc.api.token"))

  val client: Client = {
    user match {
      case None => new com.gilt.apidoc.v0.Client(apiHost, Some(apiAuth))
      case Some(u) => new com.gilt.apidoc.v0.Client(apiHost, Some(apiAuth)) {
        override def _requestHolder(path: String) = {
          super._requestHolder(path).withHeaders("X-User-Guid" -> u.guid.toString)
        }
      }
    }
  }

}
