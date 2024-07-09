package controllers

import lib.Labels
import io.apibuilder.api.v0.models.{Publication, SubscriptionForm}
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label: String = publication match {
      case Publication.MembershipRequestsCreate => "Email me when a user applies to join the org."
      case Publication.MembershipsCreate => "Email me when a user joins the org."
      case Publication.ApplicationsCreate => "Email me when an application is created."
      case Publication.VersionsCreate => Labels.SubscriptionsVersionsCreateText
      case Publication.VersionsMaterialChange => Labels.SubscriptionsVersionsMaterialChangeText
      case Publication.UNDEFINED(key) => key
    }
  }

}

class Subscriptions @Inject() (
                                val apiBuilderControllerComponents: ApiBuilderControllerComponents,
) extends ApiBuilderController {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def offerPublication(isAdmin: Boolean, publication: Publication): Boolean = {
    publication match {
      case Publication.MembershipRequestsCreate => isAdmin
      case Publication.MembershipsCreate => isAdmin
      case Publication.ApplicationsCreate => true
      case Publication.VersionsCreate => true
      case Publication.VersionsMaterialChange => true
      case Publication.UNDEFINED(_) => isAdmin
    }
  }

  def index(
    org: String
  ): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    for {
      subscriptions <- request.api.subscriptions.get(
        organizationKey = Some(request.org.key),
        userGuid = Some(request.user.guid),
        limit = Publication.all.size + 1
      )
    } yield {
      val userPublications = Publication.all.filter { p => offerPublication(request.requestData.isAdmin, p) }.map { p =>
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
  ): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    request.api.subscriptions.get(
      organizationKey = Some(request.org.key),
      userGuid = Some(request.user.guid),
      publication = Some(publication)
    ).flatMap { subscriptions =>
      subscriptions.headOption match {
        case None => {
          request.api.subscriptions.post(
            SubscriptionForm(
              organizationKey = request.org.key,
              userGuid = request.user.guid,
              publication = publication
            )
          ).map { _ =>
            Redirect(routes.Subscriptions.index(org)).flashing("success" -> "Subscription added")
          }
        }
        case Some(subscription) => {
          request.api.subscriptions.deleteByGuid(subscription.guid).map { _ =>
            Redirect(routes.Subscriptions.index(org)).flashing("success" -> "Subscription removed")
          }
        }
      }
    }
  }

}
