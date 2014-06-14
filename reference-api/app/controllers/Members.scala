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

object Members extends Controller {
  private val rowParser = new RowParser[Member] {
    def apply(row: Row): SqlResult[Member] = {
      for {
        organization <- Organizations.rowParser(row)
        user <- Users.rowParser(row)
      } yield {
        new Member(
          guid = row[UUID]("members.guid"),
          organization = organization,
          user = user,
          role = row[String]("members.role"))
      }
    }
  }

  def get(guid: Option[String], organizationGuid: Option[String], userGuid: Option[String], role: Option[String]) = Action {
    val members = DB.withConnection { implicit c =>
      SQL("""
      select * from members
      join organizations on organization = organizations.guid
      join users on user = users.guid
      where ({guid} is null or members.guid = {guid})
      and ({organization} is null or organization = {organization})
      and ({user} is null or user = {user})
      and ({role} is null or role = {role})
      """).on(
        'guid -> guid,
        'organization -> organizationGuid,
        'user -> userGuid,
        'role -> role
      ).as(rowParser.*)
    }
    Ok(Json.toJson(members))
  }

  def post() = Action(parse.json) { implicit request =>
    val json = request.body
    import play.api.libs.functional.syntax._
    json.validate[MemberForm] match {
      case JsError(_) => BadRequest {
        Json.obj(
          "code" -> "invalid_json",
          "msg" -> s"unable to parse Member from $json")
      }
      case JsSuccess(form, _) => {
        val member = DB.withConnection { implicit c =>
          val guid = UUID.randomUUID
          SQL("""
          insert into members(guid, organization, user, role)
          values ({guid}, {organization}, {user}, {role})
          """).on(
            'guid -> guid,
            'organization -> form.organization,
            'user -> form.user,
            'role -> form.role
          ).execute()
          SQL("""
          select * from members
          join organizations on organization = organizations.guid
          join users on user = users.guid
          where members.guid = {guid}
          """).on(
            'guid -> guid
          ).as(rowParser.single)
        }
        Created(Json.toJson(member))
      }
    }
  }

  def getByOrganization(organization: String) = Action {
    val members = DB.withConnection { implicit c =>
      SQL("""
      select * from members
      join organizations on organization = organizations.guid
      join users on user = users.guid
      where organization = {organization}
      """).on('organization -> organization).as(rowParser.*)
    }
    Ok(Json.toJson(members))
  }
}
