package controllers

import lib.Validation
import db.{ User, UserDao, UserPasswordDao }
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object UserAuthentications extends Controller {

  case class UserAuthentication(result: Boolean)
  object UserAuthentication {
    implicit val userAuthenticationWrites = Json.writes[UserAuthentication]
  }

  case class UserAuthenticationForm(email: String, password: String)
  object UserAuthenticationForm {
    implicit val userAuthenticationFormReads = Json.reads[UserAuthenticationForm]
  }

  private val AuthFailed = Json.toJson(UserAuthentication(false))
  private val AuthSucceeded = Json.toJson(UserAuthentication(true))

  def post() = Action(parse.json) { request =>
    request.body.validate[UserAuthenticationForm] match {
      case e: JsError => {
        Conflict(Json.toJson(Validation.error(e.toString)))
      }
      case s: JsSuccess[UserAuthenticationForm] => {
        val form = s.get
        UserDao.findByEmail(form.email) match {

          case None => {
            Ok(AuthFailed)
          }

          case Some(u: User) => {
            if (UserPasswordDao.isValid(UUID.fromString(u.guid), form.password)) {
              Ok(AuthSucceeded)
            } else {
              Ok(AuthFailed)
            }
          }

        }
      }
    }
  }

}
