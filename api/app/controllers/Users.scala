package controllers

import com.bryzek.apidoc.api.v0.models.{User, UserForm, UserUpdateForm}
import com.bryzek.apidoc.api.v0.models.json._
import lib.Validation
import db.{UsersDao, UserPasswordsDao}
import play.api.mvc._
import play.api.libs.json.{ Json, JsError, JsSuccess }
import java.util.UUID

object Users extends Controller {

  case class UserAuthenticationForm(email: String, password: String)
  object UserAuthenticationForm {
    implicit val userAuthenticationFormReads = Json.reads[UserAuthenticationForm]
  }

  def get(guid: Option[UUID], email: Option[String], token: Option[String]) = AnonymousRequest { request =>
    require(!request.tokenUser.isEmpty, "Missing API Token")
    val users = UsersDao.findAll(guid = guid.map(_.toString),
                                email = email,
                                token = token)
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: UUID) = AnonymousRequest { request =>
    require(!request.tokenUser.isEmpty, "Missing API Token")
    UsersDao.findByGuid(guid) match {
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
        UsersDao.validateNewUser(form) match {
          case Nil => {
            val user = UsersDao.create(form)
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
        UsersDao.findByGuid(guid.toString) match {

          case None => NotFound

          case Some(u: User) => {
            val existingUser = UsersDao.findByGuid(guid)

            UsersDao.validate(form, existingUser = existingUser) match {
              case Nil => {
                existingUser match {
                  case None => {
                    NotFound
                  }

                  case Some(existing) => {
                    UsersDao.update(request.user, existing, form)
                    val user =UsersDao.findByGuid(guid.toString).getOrElse {
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
        UsersDao.findByEmail(form.email) match {

          case None => {
            Conflict(Json.toJson(Validation.userAuthorizationFailed()))
          }

          case Some(u: User) => {
            if (UserPasswordsDao.isValid(u.guid, form.password)) {
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
