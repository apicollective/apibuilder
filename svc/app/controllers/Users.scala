package controllers

import core.{ User, UserQuery }
import db.UserDao
import play.api.mvc._
import play.api.libs.json.Json

object Users extends Controller {

  def get(guid: Option[String], email: Option[String], token: Option[String]) = Authenticated { request =>
    val users = UserDao.findAll(UserQuery(guid = guid,
                                          email = email,
                                          token = token))
    Ok(Json.toJson(users))
  }

  def post() = Authenticated(parse.json) { request =>
    println("POST USERS: " + request.body)
    Ok("TODO")
  }

}
