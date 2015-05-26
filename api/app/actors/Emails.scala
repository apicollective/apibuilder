package actors

import com.gilt.apidoc.api.v0.models.{Organization, Publication, Subscription}
import db.{Authorization, SubscriptionsDao}
import lib.{Config, Email, Pager, Person}
import akka.actor._
import play.api.Logger
import play.api.Play.current

object Emails {

  private lazy val sendErrorsTo = Config.requiredString("apidoc.sendErrorsTo").split("\\s+")

  def deliver(
    org: Organization,
    publication: Publication,
    subject: String,
    body: String
  ) {
    eachSubscription(org, publication, { subscription =>
      Logger.info(s"Emails: delivering email for subscription[$subscription]")
      Email.sendHtml(
        to = Person(subscription.user),
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
      SubscriptionsDao.findAll(
        Authorization.All,
        organization = Some(organization),
        publication = Some(publication),
        limit = 100,
        offset = offset
      )
    } { subscription =>
      f(subscription)
    }
  }

  def sendErrors(
    subject: String,
    errors: Seq[String]
  ) {
    errors match {
      case Nil => {}
      case errors => {
        val body = views.html.emails.errors(errors).toString
        sendErrorsTo.foreach { email =>
          Email.sendHtml(
            to = Person(email),
            subject = subject,
            body = body
          )
        }
      }
    }
  }

}
