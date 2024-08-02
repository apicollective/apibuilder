package controllers

import cats.data.Validated.{Invalid, Valid}
import db.InternalTokensDao
import io.apibuilder.api.v0.models.json.*
import io.apibuilder.api.v0.models.{CleartextToken, TokenForm}
import lib.Validation
import models.TokensModel
import play.api.libs.json.*
import play.api.mvc.*

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Tokens @Inject() (
                         val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                         tokensDao: InternalTokensDao,
                         model: TokensModel
) extends ApiBuilderController {

  def getUsersByUserGuid(
    userGuid: java.util.UUID,
    guid: Option[UUID],
    limit: Long = 25,
    offset: Long = 0
  ): Action[AnyContent] = Identified { request =>
    val tokens = model.toModels(tokensDao.findAll(
      request.authorization,
      userGuid = Some(userGuid),
      guid = guid,
      limit = Some(limit),
      offset = offset
    ))
    Ok(Json.toJson(tokens))
  }

  def getCleartextByGuid(
    guid: UUID
  ): Action[AnyContent] = Identified { request =>
    tokensDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(token) => {
        Ok(Json.toJson(CleartextToken(token.db.token)))
      }
    }
  }

  def post(): Action[JsValue] = Identified(parse.json) { request =>
    request.body.validate[TokenForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case JsSuccess(form: TokenForm, _) => {
        tokensDao.create(request.user, form) match {
          case Valid(token) => {
            Created(Json.toJson(
              model.toModel(token).getOrElse {
                sys.error("Failed to create token)")
              }
            ))
          }
          case Invalid(errors) => {
            Conflict(Json.toJson(errors.toNonEmptyList.toList))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID): Action[AnyContent] = Identified { request =>
    tokensDao.findByGuid(request.authorization, guid) match {
      case None => NotFound
      case Some(token) => {
        tokensDao.softDelete(request.user, token)
        NoContent
      }
    }
  }
}
