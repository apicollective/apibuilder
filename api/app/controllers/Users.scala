package controllers

import cats.data.Validated.{Invalid, Valid}
import io.apibuilder.api.v0.models.{User, UserForm, UserUpdateForm}
import io.apibuilder.api.v0.models.json.*
import lib.{Constants, Validation}
import util.SessionHelper
import db.{InternalUser, InternalUsersDao, UserPasswordsDao}
import models.UsersModel

import javax.inject.Inject
import play.api.libs.json.{JsArray, JsBoolean, JsError, JsObject, JsString, JsSuccess, JsValue, Json, Reads}

import java.util.UUID
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.Future

class Users @Inject() (
                        val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                        wsClient: WSClient,
                        sessionHelper: SessionHelper,
                        usersDao: InternalUsersDao,
                        userPasswordsDao: UserPasswordsDao,
                        model: UsersModel
) extends ApiBuilderController {

  import scala.concurrent.ExecutionContext.Implicits.global

  private case class UserAuthenticationForm(email: String, password: String)
  private implicit val userAuthenticationFormReads: Reads[UserAuthenticationForm] = Json.reads[UserAuthenticationForm]

  private case class GithubAuthenticationForm(token: String)
  private implicit val githubAuthenticationFormReads: Reads[GithubAuthenticationForm] = Json.reads[GithubAuthenticationForm]

  def get(
    guid: Option[UUID],
    email: Option[String],
    nickname: Option[String],
    token: Option[String]
  ): Action[AnyContent] = Identified { request =>
    if (!Seq(guid, email, nickname, token).exists(_.isDefined)) {
      // require system user to show more then one user
      requireSystemUser(request.user)
    }

    val users = usersDao.findAll(
      guid = guid,
      email = email,
      nickname = nickname,
      token = token,
      limit = None
    )
    Ok(Json.toJson(model.toModels(users)))
  }

  def getByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    requireSystemUser(request.user)
    usersDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user) => Ok(Json.toJson(model.toModel(user)))
    }
  }

  def post(): Action[JsValue] = Anonymous(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: UserForm, _) => {
        usersDao.create(form) match {
          case Valid(user) => Ok(Json.toJson(model.toModel(user)))
          case Invalid(errors) => Conflict(Json.toJson(errors.toNonEmptyList.toList))
        }
      }
    }
  }

  def putByGuid(guid: UUID): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[UserUpdateForm] match {
      case e: JsError => Conflict(Json.toJson(Validation.invalidJson(e)))
      case JsSuccess(form: UserUpdateForm, _) => {
        usersDao.findByGuid(guid) match {
          case None => NotFound
          case Some(existingUser) => {
            usersDao.update(request.user, existingUser, form) match {
              case Valid(user) => Ok(Json.toJson(model.toModel(user)))
              case Invalid(errors) => Conflict(Json.toJson(errors.toNonEmptyList.toList))
            }
          }
        }
      }
    }
  }

  def postAuthenticate(): Action[JsValue] = Anonymous(parse.json) { request =>
    request.body.validate[UserAuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: UserAuthenticationForm, _) => {
        usersDao.findByEmail(form.email) match {
          case None => {
            Conflict(Json.toJson(Validation.userAuthorizationFailed()))
          }

          case Some(user) => {
            if (userPasswordsDao.isValid(user.guid, form.password)) {
              Ok(Json.toJson(sessionHelper.createAuthentication(user)))
            } else {
              Conflict(Json.toJson(Validation.userAuthorizationFailed()))
            }
          }
        }
      }
    }
  }

  def postAuthenticateGithub(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[GithubAuthenticationForm] match {
      case e: JsError => Future.successful {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: GithubAuthenticationForm, _) => {
        val headers = "Authorization" -> s"Bearer ${form.token}"

        for {
          userResponse <- wsClient.url("https://api.github.com/user").addHttpHeaders(headers).get()

          emailsResponse <- wsClient.url("https://api.github.com/user/emails").addHttpHeaders(headers).get()
        } yield {
          val obj = Json.parse(userResponse.body).as[JsObject]
          val login = (obj \ "login").asOpt[String].getOrElse {
            sys.error(s"Failed to get github user login. Response: $obj")
          }

          val emails = Json.parse(emailsResponse.body).as[JsArray]
          val primaryEmailObject = emails.value.map(_.as[JsObject]).find { js =>
            (js \ "primary").as[JsBoolean].value
          }.getOrElse {
            sys.error("Github user does not have a primary email address: " + Json.prettyPrint(emails))
          }
          val email = (primaryEmailObject \ "email").as[JsString].value

          val user = usersDao.findByEmail(email).getOrElse {
            usersDao.createForGithub(
              login = login,
              email = email,
              name = (obj \ "name").asOpt[String],
              avatarUrl = (obj \ "avatar_url").asOpt[String],
              gravatarId = (obj \ "gravatar_id").asOpt[String]
            )
          }

          Ok(Json.toJson(sessionHelper.createAuthentication(user)))
        }
      }
    }
  }

  private def requireSystemUser(user: InternalUser): Unit = {
    require(
      user.guid == Constants.AdminUserGuid,
      "Action restricted to the system admin user"
    )
  }
}
