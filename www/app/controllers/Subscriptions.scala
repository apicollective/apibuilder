package controllers

import com.gilt.apidoc.models.{Publication, Subscription, SubscriptionForm}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Subscriptions extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.MembershipRequestsCreate => "For organizations for which I am an administrator, email me whenever a user applies to join the org."
      case Publication.MembershipsCreate => "For organizations for which I am a member, email me whenever a user join the org."
      case Publication.ServicesCreate => "For organizations for which I am a member, email me whenever a service is created."
      case Publication.VersionsCreate => "For services that I watch, email me whenever a version is created."
      case Publication.UNDEFINED(key) => key
    }
  }

  def index(
    org: String
  ) = AuthenticatedOrg.async { implicit request =>
    for {
      subscriptions <- request.api.subscriptions.get(
        organizationKey = Some(request.org.key),
        userGuid = Some(request.user.guid),
        limit = Some(Publication.all.size + 1)
      )
    } yield {
      val userPublications = Publication.all.map { p =>
        UserPublication(
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
