package controllers

import db.{Authorization, SubscriptionsDao}
import lib.Validation
import com.gilt.apidoc.api.v0.models.{Publication, Subscription, SubscriptionForm, User}
import com.gilt.apidoc.api.v0.models.json._
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Subscriptions extends Controller with Subscriptions

trait Subscriptions {
  this: Controller =>

  def get(
    guid: Option[UUID],
    organizationKey: Option[String],
    userGuid: Option[java.util.UUID],
    publication: Option[Publication],
    limit: Long = 25,
    offset: Long = 0
  ) = Authenticated { request =>
    val subscriptions = SubscriptionsDao.findAll(
      Authorization(Some(request.user)),
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
    SubscriptionsDao.findByUserAndGuid(request.user, guid) match {
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
        SubscriptionsDao.validate(request.user, form) match {
          case Nil => {
            val subscription = SubscriptionsDao.create(request.user, form)
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
    SubscriptionsDao.findByUserAndGuid(request.user, guid).map { subscription =>
      SubscriptionsDao.softDelete(request.user, subscription)
    }
    NoContent
  }


}
