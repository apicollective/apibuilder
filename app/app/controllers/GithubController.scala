package controllers

import lib.Github
import play.api.{Logger, Logging}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.core.parsers.FormUrlEncodedParser

import scala.concurrent.Future

class GithubController @javax.inject.Inject() (
                                                val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                                ws: WSClient,
                                                github: Github
) extends ApiBuilderController with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  def redirect(returnUrl: Option[String]): Action[AnyContent] = Action {
    Redirect(routes.GithubController.index(returnUrl))
  }

  def index(returnUrl: Option[String]): Action[AnyContent] = Action {
    Redirect(github.oauthUrl(returnUrl = returnUrl))
  }

  def callback(
    code: String,
    returnUrl: String
  ): Action[AnyContent] = Anonymous.async { request =>
    getAccessToken(code).flatMap {
      case Left(ex) => Future.successful {
        logger.error(s"Unable to process git hub login: ${ex.getMessage}", ex)
        Redirect(
          routes.LoginController.index(return_url = Some(returnUrl))
        ).flashing("warning" -> s"GitHub login failed - please try again")
      }
      case Right(token) => {
        request.api.users.postAuthenticateGithub(token).map { auth =>
          Redirect(returnUrl).withSession { "session_id" -> auth.session.id }
        }.recover {
          case ex: Throwable => {
            logger.error(s"Api failed to authenticate user with valid github token: ${ex.getMessage}", ex)
            Redirect(
              routes.LoginController.index(return_url = Some(returnUrl))
            ).flashing("warning" -> s"GitHub login failed - please try again")
          }
        }
      }
    }
  }

  private def getAccessToken(code: String): Future[Either[Throwable, String]] = {
    val form = Json.obj(
      "client_id" -> github.clientId,
      "client_secret" -> github.clientSecret,
      "code" -> code
    )

    ws.url("https://github.com/login/oauth/access_token").post(form).map { result =>
      val parsed = FormUrlEncodedParser.parse(result.body)
      val accessToken = parsed.get("access_token").getOrElse {
        sys.error(s"GitHub Oauth response did not contain an access_token: ${result.body}")
      }.headOption.getOrElse {
        sys.error(s"GitHub Oauth response returned an empty list for access_token: ${result.body}")
      }
      Right(accessToken)

    }.recover {
      case ex: Throwable => {
        Left(ex)
      }
    }
  }
}

