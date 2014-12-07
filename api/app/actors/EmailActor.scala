package actors

import com.gilt.apidoc.models.{Publication, Subscription}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import db.{Pager, SubscriptionDao}
import lib.{Email, Person}
import akka.actor._
import play.api.Logger
import play.api.Play.current

object EmailActor {

  object Messages {
    case class MembershipRequestCreated(id: Long)
  }

}

class EmailActor extends Actor {

  def receive = {

    case MainActor.Messages.MembershipRequestCreated(id) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestCreated(id)", {
        MembershipRequestDao.findById(id).map { mr =>
          Emails.deliver(
            org = incident.organization,
            publication = publication,
            subject = s"${mr.organization.key} Membership Request from ${mr.user.email}",
            body = views.html.emails.membershipRequestCreated(mr).toString
          )
        }
      }
    )

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }
}

