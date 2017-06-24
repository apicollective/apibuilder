package controllers

import db.{Authorization, TokensDao}
import lib.Validation
import io.apibuilder.apidoc.api.v0.models.{User, Token, TokenForm}
import io.apibuilder.apidoc.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Tokens @Inject() (
  tokensDao: TokensDao
) extends Controller {

  def getUsersByUserGuid(
    userGuid: java.util.UUID,
    guid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val tokens = tokensDao.findAll(
      request.authorization,
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
    tokensDao.findCleartextByGuid(request.authorization, guid) match {
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
        tokensDao.validate(request.user, form) match {
          case Nil => {
            val token = tokensDao.create(request.user, form)
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
    tokensDao.findByGuid(request.authorization, guid).map { token =>
      tokensDao.softDelete(request.user, token)
    }
    NoContent
  }

}
