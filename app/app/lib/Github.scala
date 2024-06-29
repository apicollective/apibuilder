package lib

import java.net.URLEncoder
import javax.inject.Inject

class Github @Inject() (
  config: Config,
  util: Util
) {

  lazy val clientId: String = config.requiredString("apibuilder.github.oauth.client.id")
  lazy val clientSecret: String = config.requiredString("apibuilder.github.oauth.client.secret")
  private lazy val baseUrl = util.fullUrl("/login/github/callback")

  private val Scopes = Seq("user:email")

  private val OauthUrl = "https://github.com/login/oauth/authorize"

  def oauthUrl(returnUrl: Option[String]): String = {
    val finalUrl = URLEncoder.encode(
      util.fullUrl(returnUrl.getOrElse("/")),
      "UTF-8"
    )

    OauthUrl + "?" + Seq(
      "scope" -> Scopes.mkString(","),
      "client_id" -> clientId,
      "redirect_uri" -> s"$baseUrl?return_url=$finalUrl"
    ).map { case (key, value) =>
      s"$key=" + URLEncoder.encode(value, "UTF-8")
    }.mkString("&")
  }

}

