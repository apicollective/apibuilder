package controllers

import lib.Util
import com.bryzek.apidoc.api.v0.models.{Publication, Subscription, SubscriptionForm}
import scala.concurrent.Await
import scala.concurrent.duration._

import play.api.data._
import play.api.data.Forms._

import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.MembershipRequestsCreate => "Email me when a user applies to join the org."
      case Publication.MembershipsCreate => "Email me when a user joins the org."
      case Publication.ApplicationsCreate => "Email me when an application is created."
      case Publication.VersionsCreate => Util.SubscriptionsVersionsCreateText
      case Publication.UNDEFINED(key) => key
    }
  }

}

class Subscriptions @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def offerPublication(isAdmin: Boolean, publication: Publication): Boolean = {
    publication match {
      case Publication.MembershipRequestsCreate => isAdmin
      case Publication.MembershipsCreate => isAdmin
      case Publication.ApplicationsCreate => true
      case Publication.VersionsCreate => true
      case Publication.UNDEFINED(key) => isAdmin
    }
  }

  def index(
    org: String
  ) = AuthenticatedOrg.async { implicit request =>
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
          isSubscribed = !subscriptions.find(_.publication == p).isEmpty
        )
      }
      Ok(views.html.subscriptions.index(request.mainTemplate(), userPublications))
    }
  }

  def postToggle(
    org: String,
    publication: Publication
  ) = AuthenticatedOrg.async { implicit request =>
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
