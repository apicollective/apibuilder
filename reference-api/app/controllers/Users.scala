package controllers

import java.util.UUID

import play.api.Play.current
import play.api.db._
import play.api.mvc._
import play.api.libs.json._

import referenceapi.models._
import referenceapi.models.json._

import anorm._
import anorm.SqlParser._

import db.Implicits._

object Users extends Controller {
  val rowParser = new RowParser[User] {
    def apply(row: Row): SqlResult[User] = Success {
      new user.UserImpl(
        guid = row[UUID]("users.guid"),
        email = row[String]("users.email"),
        active = row[Boolean]("users.active")
      )
    }
  }

  def get(guid: Option[String], email: Option[String], active: Boolean) = Action {
    val us = DB.withConnection { implicit c =>
      SQL("""
      select * from users
      where ({guid} is null or guid = {guid})
      and ({email} is null or email = {email})
      and active = {active}
      """).on(
        'guid -> guid.map(UUID.fromString),
        'email -> email,
        'active -> active
      ).as(rowParser *)
    }
    Ok(Json.toJson(us))
  }

  def post() = Action(parse.json) { implicit request =>
    val json = request.body
    json.validate[User] match {
      case JsSuccess(u, _) => {
        DB.withConnection { implicit c =>
          SQL("""
          insert into users(guid, email, active)
          values({guid}, {email}, {active})
          """).on(
            'guid -> u.guid,
            'email -> u.email,
            'active -> u.active
          ).execute()
        }
        Created(Json.toJson(u))
      }

      case JsError(es) => {
        BadRequest {
          Json.obj(
            "code" -> "invalid_json",
            "msg" -> s"unable to parse User from $json"
          )
        }
      }
    }
  }

  def postNoop() = Action { Ok("") }
}
