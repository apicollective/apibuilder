package controllers

import java.util.UUID

import play.api._
import play.api.mvc._

import play.api.libs.json._

import referenceapi.models._

object Members extends Controller {
  def get(guid: Option[String], organizationGuid: Option[String], userGuid: Option[String], role: Option[String]) = Action {
    Ok(
      Json.toJson(
        List(
          new member.MemberImpl(
            guid = UUID.fromString(guid.get),
            organization = null,
            user = null,
            role = role.get
          )
        )
      )
    )
  }

  def post() = Action(parse.json) { implicit request =>
    Created(Json.toJson(request.body.as[Member]))
  }
}
