package controllers

import java.util.UUID

import play.api.Play.current
import play.api.db._
import play.api.mvc._
import play.api.libs.json._

import referenceapi.models._

import anorm._
import anorm.SqlParser._

import db.Implicits._

object Organizations extends Controller {
  val rowParser = new RowParser[Organization] {
    def apply(row: Row): SqlResult[Organization] = Success {
      new organization.OrganizationImpl(
        guid = row[UUID]("organizations.guid"),
        name = row[String]("organizations.name")
      )
    }
  }

  def get(guid: Option[String], name: Option[String]) = Action {
    val os = DB.withConnection { implicit c =>
      SQL("""
      select * from organizations
      where ({guid} is null or guid = {guid})
      and ({name} is null or name = {name})
      """).on(
        'guid -> guid.map(UUID.fromString),
        'name -> name
      ).as(rowParser.*)
    }
    Ok(Json.toJson(os))
  }

  def post() = Action(parse.json) { implicit request =>
    val json = request.body
    json.validate[Organization] match {
      case JsSuccess(o, _) => {
        DB.withConnection { implicit c =>
          SQL("""
          insert into organizations(guid, name)
          values({guid}, {name})
          """).on(
            'guid -> o.guid,
            'name -> o.name
          ).execute()
        }
        Created(Json.toJson(o))
      }

      case JsError(es) => {
        BadRequest {
          Json.obj(
            "code" -> "invalid_json",
            "msg" -> s"unable to parse Organization from $json"
          )
        }
      }
    }
  }

  def getByGuid(guid: String) = Action {
    DB.withConnection { implicit c =>
      SQL("""
      select * from organizations where guid = {guid}
      """).on('guid -> UUID.fromString(guid)).as(rowParser.singleOpt)
    } match {
      case None => NotFound
      case Some(o) => Ok(Json.toJson(o))
    }
  }
}
