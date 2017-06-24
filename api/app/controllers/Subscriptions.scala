package controllers

import db.{Authorization, SubscriptionsDao}
import lib.Validation
import io.apibuilder.api.v0.models.{Publication, Subscription, SubscriptionForm, User}
import io.apibuilder.api.v0.models.json._
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

@Singleton
class Subscriptions @Inject() (
  subscriptionsDao: SubscriptionsDao
) extends Controller {

  def get(
    guid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[java.util.UUID],
    publication: Option[Publication],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val subscriptions = subscriptionsDao.findAll(
      request.authorization,
      guid = guid,
      organizationKey = organizationKey,
      userGuid = userGuid,
      publication = publication,
      limit = limit,
      offset = offset
    )
    Ok(Json.toJson(subscriptions))
  }

  def getByGuid(guid: UUID) = Authenticated { request =>
    subscriptionsDao.findByUserAndGuid(request.user, guid) match {
      case None => NotFound
      case Some(subscription) => Ok(Json.toJson(subscription))
    }
  }

  def post() = Authenticated(parse.json) { request =>
    request.body.validate[SubscriptionForm] match {
      case e: JsError => {
        BadRequest(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[SubscriptionForm] => {
        val form = s.get
        subscriptionsDao.validate(request.user, form) match {
          case Nil => {
            val subscription = subscriptionsDao.create(request.user, form)
            Created(Json.toJson(subscription))
          }
          case errors => {
            Conflict(Json.toJson(errors))
          }
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    subscriptionsDao.findByUserAndGuid(request.user, guid).map { subscription =>
      subscriptionsDao.softDelete(request.user, subscription)
    }
    NoContent
  }


}
