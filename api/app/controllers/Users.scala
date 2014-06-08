package controllers

import db.{ User, UserDao }
import play.api.mvc._
import play.api.libs.json.Json

object Users extends Controller {

  def get(guid: Option[String], email: Option[String], token: Option[String]) = Authenticated { request =>
    val users = UserDao.findAll(guid = guid,
                                email = email,
                                token = token)
    Ok(Json.toJson(users))
  }

  def getByGuid(guid: String) = Authenticated { request =>
    UserDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user: User) => Ok(Json.toJson(user))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    (request.body \ "email").asOpt[String] match {
      case None => {
        BadRequest("email is required")
      }

      case Some(email: String) => {
        UserDao.findByEmail(email) match {
          case None => {
            val user = UserDao.upsert(email = email,
                                      name = (request.body \ "name").asOpt[String],
                                      imageUrl = (request.body \ "image_url").asOpt[String])
            Ok(Json.toJson(user))
          }

          case Some(u: User) => {
            BadRequest("user with this email already exists")
          }
        }
      }
    }
  }

  def put(guid: String) = Authenticated(parse.json) { request =>
    UserDao.findByGuid(guid) match {
      case None => NotFound
      case Some(user: User) => {
        val newUser = user.copy(email = (request.body \ "name").asOpt[String].getOrElse(user.email),
                                name = (request.body \ "name").asOpt[String],
                                imageUrl = (request.body \ "image_url").asOpt[String])
        UserDao.update(newUser)
        Ok(Json.toJson(newUser))
      }
    }
  }

}
