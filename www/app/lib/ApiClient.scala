package lib

import com.gilt.apidoc.api.v0.models.User
import com.gilt.apidoc.api.v0.{Authorization, Client}

case class ApiClient(user: Option[User]) {

  private val apiHost = Config.requiredString("apidoc.api.host")
  private val apiAuth = Authorization.Basic(Config.requiredString("apidoc.api.token"))

  val client: Client = {
    val defaultHeaders = Seq(
      user.map { u =>
        ("X-User-Guid", u.guid.toString)
      }
    ).flatten

    new com.gilt.apidoc.api.v0.Client(
      apiUrl = apiHost,
      auth = Some(apiAuth),
      defaultHeaders = defaultHeaders
    )
  }

}
