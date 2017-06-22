package controllers

import com.bryzek.apidoc.api.v0.models.{Authentication, Session, User, UserForm, UserUpdateForm}
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import util.{Conversions, SessionIdGenerator}
import db.{UserPasswordsDao, UsersDao}
import db.generated.SessionsDao
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime

import play.api.mvc._
import play.api.libs.json.{JsArray, JsBoolean, JsError, JsObject, JsString, JsSuccess, Json}
import java.util.UUID

import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Singleton
class Users @Inject() (
  sessionsDao: SessionsDao,
  usersDao: UsersDao,
  userPasswordsDao: UserPasswordsDao,
  ws: WSClient
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] val DefaultSessionExpirationHours = 24 * 30

  private[this] case class UserAuthenticationForm(email: String, password: String)
  private[this] object UserAuthenticationForm {
    implicit val userAuthenticationFormReads = Json.reads[UserAuthenticationForm]
  }

  private[this] case class GithubAuthenticationForm(token: String)
  private[this] object GithubAuthenticationForm {
    implicit val githubAuthenticationFormReads = Json.reads[GithubAuthenticationForm]
  }

  def get(guid: Option[UUID], email: Option[String], token: Option[String]) = AnonymousRequest { request =>
    require(request.tokenUser.isDefined, "Missing API Token")
    val users = usersDao.findAll(
      guid = guid.map(_.toString),
      email = email,
      token = token
    )
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: UUID) = AnonymousRequest { request =>
    require(request.tokenUser.isDefined, "Missing API Token")
    usersDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user: User) => Ok(Json.toJson(user))
    }
  }

  def post() = AnonymousRequest(parse.json) { request =>
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

  def putByGuid(guid: UUID) = Authenticated(parse.json) { request =>
    request.body.validate[UserUpdateForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserUpdateForm] => {
        val form = s.get
        usersDao.findByGuid(guid.toString) match {

          case None => NotFound

          case Some(u: User) => {
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

  def postAuthenticate() = AnonymousRequest(parse.json) { request =>
    println(s"postAuthenticate")
    request.body.validate[UserAuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserAuthenticationForm] => {
        val form = s.get
        println(s"postAuthenticate: $form")

        usersDao.findByEmail(form.email) match {

          case None => {
            Conflict(Json.toJson(Validation.userAuthorizationFailed()))
          }

          case Some(u: User) => {
            if (userPasswordsDao.isValid(u.guid, form.password)) {
              Ok(Json.toJson(createAuthentication(u)))
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

        for {
          userResponse <- ws.url("https://api.github.com/user").
          withHeaders(
            "Authentication" -> s"Bearer $token"
          ).get()

          emailsResponse <- ws.url("https://api.github.com/user/emails").
          withHeaders(
            "Authentication" -> s"Bearer $token"
          ).get()
        } yield {
          val obj = Json.parse(userResponse.body).as[JsObject]
          play.api.Logger.info(s"GITHUB USER: " + Json.prettyPrint(obj))

          val emails = Json.parse(emailsResponse.body).as[JsArray]
          play.api.Logger.info(s"GITHUB USER EMAILS: " + Json.prettyPrint(emails))
          val primaryEmailObject = emails.value.map(_.as[JsObject]).find { js =>
            (js \ "primary").as[JsBoolean].value
          }.getOrElse {
            sys.error("Github user does not have a primary email address: " + Json.prettyPrint(emails))
          }
          val email = (primaryEmailObject \ "email").as[JsString].value

          val user = usersDao.findByEmail(email).getOrElse {
            usersDao.createForGithub(
              login = (obj \ "login").as[String],
              email = email,
              name = (obj \ "name").asOpt[String],
              avatarUrl = (obj \ "avatar_url").asOpt[String],
              gravatarId = (obj \ "gravatar_id").asOpt[String]
            )
          }
          Ok(Json.toJson(createAuthentication(user)))
        }
      }
    }
  }

  private[this] def createAuthentication(u: User): Authentication = {
    val id = SessionIdGenerator.generate()

    sessionsDao.insert(
      usersDao.AdminUser.guid,
      _root_.db.generated.SessionForm(
        id = id,
        userGuid = u.guid,
        expiresAt = DateTime.now().plusHours(DefaultSessionExpirationHours)
      )
    )

    val dbSession = sessionsDao.findById(id).getOrElse {
      sys.error("Failed to create session")
    }

    Conversions.toAuthentication(dbSession, u)
  }
}
