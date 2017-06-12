package lib

import java.net.URLEncoder

object Github {

  lazy val clientId: String = Config.requiredString("apibuilder.github.oauth.client.id")
  lazy val clientSecret: String = Config.requiredString("apibuilder.github.oauth.client.secret")
  private[this] lazy val baseUrl = Util.fullUrl("/login/github/callback")

  private[this] val Scopes = Seq("user:email")

  private[this] val OauthUrl = "https://github.com/login/oauth/authorize"

  def oauthUrl(returnUrl: Option[String]): String = {
    val finalUrl = URLEncoder.encode(
      Util.fullUrl(returnUrl.getOrElse("/")),
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

