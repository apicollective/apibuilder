package controllers

import db.{Authorization, TokensDao}
import lib.Validation
import com.gilt.apidoc.v0.models.{User, Token, TokenForm}
import com.gilt.apidoc.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Tokens extends Controller with Tokens

trait Tokens {
  this: Controller =>

  def getUsersByUserGuid(
    userGuid: java.util.UUID,
    guid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val tokens = TokensDao.findAll(
      Authorization(Some(request.user)),
      userGuid = Some(userGuid),
      guid = guid,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(tokens))
  }

  def getCleartextByGuid(
    guid: UUID
  ) = Authenticated { request =>
    TokensDao.findCleartextByGuid(guid) match {
      case None => NotFound
      case Some(token) => {
        Ok(Json.toJson(token))
      }
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[TokenForm] match {
      case e: JsError => {
        BadRequest(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[TokenForm] => {
        val form = s.get
        TokensDao.validate(request.user, form) match {
          case Nil => {
            val token = TokensDao.create(request.user, form)
            Created(Json.toJson(token))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    TokensDao.findByGuid(Authorization(Some(request.user)), guid).map { token =>
      TokensDao.softDelete(request.user, token)
    }
    NoContent
  }

}
