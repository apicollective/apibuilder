package controllers

import com.gilt.apidoc.models.User
import com.gilt.apidoc.models.json._
import lib.Validation
import db.{UserForm, UserDao, UserPasswordDao}
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
    val users = UserDao.findAll(guid = guid.map(_.toString),
                                email = email,
                                token = token)
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: UUID) = AnonymousRequest { request =>
    require(!request.tokenUser.isEmpty, "Missing API Token")
    UserDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user: User) => Ok(Json.toJson(user))
    }
  }

  def post() = Action(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[UserForm] => {
        val form = s.get
        UserDao.findByEmail(form.email) match {

          case Some(u: User) => {
            Conflict(Json.toJson(Validation.error("account with this email already exists")))
          }

          case None => {
            val user = UserDao.create(form)
            Ok(Json.toJson(user))
          }
        }
      }
    }
  }

  def putByGuid(guid: UUID) = Authenticated(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[UserForm] => {
        val form = s.get
        UserDao.findByGuid(guid.toString) match {

          case None => NotFound

          case Some(u: User) => {
            val existingUser = UserDao.findByEmail(form.email)
            if (existingUser.isEmpty || existingUser.get.guid == guid.toString) {
              UserDao.update(request.user, u, form)
              val updatedUser = UserDao.findByGuid(guid.toString).get
              Ok(Json.toJson(updatedUser))
            } else {
              Conflict(Json.toJson(Validation.error("account with this email already exists")))
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
        Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
      }
      case s: JsSuccess[UserAuthenticationForm] => {
        val form = s.get
        UserDao.findByEmail(form.email) match {

          case None => {
            Conflict(Json.toJson(Validation.userAuthorizationFailed()))
          }

          case Some(u: User) => {
            if (UserPasswordDao.isValid(u.guid, form.password)) {
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
