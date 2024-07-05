package controllers

import db.TokensDao
import lib.Validation
import io.apibuilder.api.v0.models.TokenForm
import io.apibuilder.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Tokens @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
  tokensDao: TokensDao
) extends ApiBuilderController {

  def getUsersByUserGuid(
    userGuid: java.util.UUID,
    guid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ) = Identified.async { request =>
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
  ) = Identified.async { request =>
    tokensDao.findCleartextByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(token) => {
        Ok(Json.toJson(token))
      }
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[TokenForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
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

  def deleteByGuid(guid: UUID) = Identified.async { request =>
    tokensDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(token) => {
        tokensDao.softDelete(request.user, token)
        NoContent
      }
    }
  }
}
