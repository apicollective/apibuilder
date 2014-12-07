package actors

import com.gilt.apidoc.models.{Organization, Publication}
import db.{Pager, SubscriptionsDao}
import lib.{Email, Person}
import akka.actor._
import play.api.Logger
import play.api.Play.current

object Emails {

  def deliver(
    org: Organization,
    publication: Publication,
    subject: String,
    body: String
  ) {
    eachSubscription(org, publication, { subscription =>
      Logger.info(s"Emails: delivering email for subscription[$subscription]")
      Email.sendHtml(
        to = Person(
          email = subscription.user.email,
          name = subscription.user.name
        ),
        subject = subject,
        body = body
      )
    })
  }

  def eachSubscription(
    organization: Organization,
    publication: Publication,
    f: Subscription => Unit
  ) {
    Pager.eachPage[Subscription] { offset =>
      SubscriptionDao.findAll(
        organization = Some(organization),
        publication = Some(publication),
        limit = 100,
        offset = offset
      )
    } { subscription =>
      f(subscription)
    }
  }

}
