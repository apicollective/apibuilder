package controllers

import com.bryzek.apidoc.api.v0.models.{User, UserForm, UserUpdateForm}
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.{UsersDao, UserPasswordsDao}
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.{ Json, JsError, JsSuccess }
import java.util.UUID

@Singleton
class Users @Inject() (
  usersDao: UsersDao,
  userPasswordsDao: UserPasswordsDao
) extends Controller {

  case class UserAuthenticationForm(email: String, password: String)
  object UserAuthenticationForm {
    implicit val userAuthenticationFormReads = Json.reads[UserAuthenticationForm]
  }

  def get(guid: Option[UUID], email: Option[String], token: Option[String]) = AnonymousRequest { request =>
    require(!request.tokenUser.isEmpty, "Missing API Token")
    val users = usersDao.findAll(guid = guid.map(_.toString),
                                email = email,
                                token = token)
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: UUID) = AnonymousRequest { request =>
    require(!request.tokenUser.isEmpty, "Missing API Token")
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
    require(!request.tokenUser.isEmpty, "Missing API Token")

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

          case Some(u: User) => {
            if (userPasswordsDao.isValid(u.guid, form.password)) {
              Ok(Json.toJson(u))
            } else {
              Conflict(Json.toJson(Validation.userAuthorizationFailed()))
            }
          }
        }
      }
    }
  }

}
