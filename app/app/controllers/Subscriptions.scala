package controllers

import lib.{ApiClientProvider, Labels}
import io.apibuilder.api.v0.models.{Publication, SubscriptionForm}

import scala.concurrent.Await
import scala.concurrent.duration._
import javax.inject.Inject

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.MembershipRequestsCreate => "Email me when a user applies to join the org."
      case Publication.MembershipsCreate => "Email me when a user joins the org."
      case Publication.ApplicationsCreate => "Email me when an application is created."
      case Publication.VersionsCreate => Labels.SubscriptionsVersionsCreateText
      case Publication.UNDEFINED(key) => key
    }
  }

}

class Subscriptions @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  apiClientProvider: ApiClientProvider
) extends ApibuilderController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def offerPublication(isAdmin: Boolean, publication: Publication): Boolean = {
    publication match {
      case Publication.MembershipRequestsCreate => isAdmin
      case Publication.MembershipsCreate => isAdmin
      case Publication.ApplicationsCreate => true
      case Publication.VersionsCreate => true
      case Publication.UNDEFINED(_) => isAdmin
    }
  }

  def index(
    org: String
  ) = IdentifiedOrg.async { implicit request =>
    for {
      subscriptions <- request.api.subscriptions.get(
        organizationKey = Some(request.org.key),
        userGuid = Some(request.user.guid),
        limit = Publication.all.size + 1
      )
    } yield {
      val userPublications = Publication.all.filter { p => offerPublication(request.isAdmin, p) }.map { p =>
        Subscriptions.UserPublication(
          publication = p,
          isSubscribed = subscriptions.exists(_.publication == p)
        )
      }
      Ok(views.html.subscriptions.index(request.mainTemplate(), userPublications))
    }
  }

  def postToggle(
    org: String,
    publication: Publication
  ) = IdentifiedOrg.async { implicit request =>
    for {
      subscriptions <- request.api.subscriptions.get(
        organizationKey = Some(request.org.key),
        userGuid = Some(request.user.guid),
        publication = Some(publication)
      )
    } yield {
      subscriptions.headOption match {
        case None => {
          Await.result(
            request.api.subscriptions.post(
              SubscriptionForm(
                organizationKey = request.org.key,
                userGuid = request.user.guid,
                publication = publication
              )
            ),
            1000.millis
          )
          Redirect(routes.Subscriptions.index(org)).flashing("success" -> "Subscription added")
        }
        case Some(subscription) => {
          Await.result(
            request.api.subscriptions.deleteByGuid(subscription.guid), 1000.millis
          )
          Redirect(routes.Subscriptions.index(org)).flashing("success" -> "Subscription removed")
        }
      }
    }

  }

}
