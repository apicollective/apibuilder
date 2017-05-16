package lib

import java.net.URLEncoder

object Github {

  lazy val clientId = Config.requiredString("apibuilder.github.oauth.client.id")
  lazy val clientSecret = Config.requiredString("apibuilder.github.oauth.client.secret")
  lazy val baseUrl = Util.fullUrl("/login/github")

  private[this] val Scopes = Seq("user:email", "repo", "read:repo_hook", "write:repo_hook")

  private[this] val OauthUrl = "https://github.com/login/oauth/authorize"

  def oauthUrl(returnUrl: Option[String]): String = {
    OauthUrl + "?" + Seq(
      Some("scope" -> Scopes.mkString(",")),
      Some("client_id" -> clientId),
      returnUrl.map { url => ("redirect_uri" -> (s"$baseUrl?return_url=" + URLEncoder.encode(url, "UTF-8"))) }
    ).flatten.map { case (key, value) =>
        s"$key=" + URLEncoder.encode(value, "UTF-8")
    }.mkString("&")
  }

}

