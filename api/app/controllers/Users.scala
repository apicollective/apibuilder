package controllers

import io.apibuilder.api.v0.models.{User, UserForm, UserUpdateForm}
import io.apibuilder.api.v0.models.json._
import lib.Validation
import util.SessionHelper
import db.{UserPasswordsDao, UsersDao}

import javax.inject.Inject
import play.api.libs.json.{JsArray, JsBoolean, JsError, JsObject, JsString, JsSuccess, Json, Reads}

import java.util.UUID
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class Users @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  wsClient: WSClient,
  sessionHelper: SessionHelper,
  usersDao: UsersDao,
  userPasswordsDao: UserPasswordsDao
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
  ) = Identified.async { request =>
    if (!Seq(guid, email, nickname, token).exists(_.isDefined)) {
      // require system user to show more then one user
      requireSystemUser(request.user)
    }

    val users = usersDao.findAll(
      guid = guid,
      email = email,
      nickname = nickname,
      token = token
    )
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: UUID) = Identified.async { request =>
    requireSystemUser(request.user)
    usersDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user: User) => Ok(Json.toJson(user))
    }
  }

  def post() = Anonymous(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserForm] => {
        val form = s.get
        usersDao.validateNewUser(form) match {
          case Nil => {
            val user = usersDao.create(form)
            Ok(Json.toJson(user))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def putByGuid(guid: UUID) = Identified(parse.json) { request =>
    request.body.validate[UserUpdateForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserUpdateForm] => {
        val form = s.get
        usersDao.findByGuid(guid.toString) match {

          case None => NotFound

          case Some(u: User) if u.guid != request.user.guid=> Unauthorized

          case Some(_: User) => {
            val existingUser = usersDao.findByGuid(guid)

            usersDao.validate(form, existingUser = existingUser) match {
              case Nil => {
                existingUser match {
                  case None => {
                    NotFound
                  }

                  case Some(existing) => {
                    usersDao.update(request.user, existing, form)
                    val user =usersDao.findByGuid(guid.toString).getOrElse {
                      sys.error("Failed to update user")
                    }
                    Ok(Json.toJson(user))
                  }
                }
              }
              case errors => {
                Conflict(Json.toJson(errors))
              }
            }
          }
        }
      }
    }
  }

  def postAuthenticate() = Anonymous(parse.json) { request =>
    request.body.validate[UserAuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserAuthenticationForm] => {
        val form = s.get
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

  def postAuthenticateGithub() = Action.async(parse.json) { request =>
    request.body.validate[GithubAuthenticationForm] match {
      case e: JsError => Future.successful {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GithubAuthenticationForm] => {
        val token = s.get.token
        val headers = "Authorization" -> s"Bearer $token"

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

  private def requireSystemUser(user: User): Unit = {
    require(
      user.guid == UsersDao.AdminUserGuid,
      "Action restricted to the system admin user"
    )
  }
}
