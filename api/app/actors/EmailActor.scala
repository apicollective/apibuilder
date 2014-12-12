package actors

import com.gilt.apidoc.models.{Membership, Publication}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import db.{MembershipRequestDao}
import lib.{Email, Pager, Person, Role}
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object EmailActor {

  object Messages {
    case class MembershipRequestCreated(guid: UUID)
    case class MembershipCreated(guid: UUID)
  }

}

class EmailActor extends Actor {

  def receive = {

    case EmailActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipRequestCreated($guid)", {
        MembershipRequestDao.findByGuid(guid).map { request =>
          Emails.deliver(
            org = request.organization,
            publication = Publication.MembershipRequestsCreate,
            subject = s"${request.organization.name}: Membership Request from ${request.user.email}",
            body = views.html.emails.membershipRequestCreated(request).toString
          )
        }
      }
    )

    case EmailActor.Messages.MembershipCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipCreated($guid)", {
        db.Membership.findByGuid(guid).map { membership =>
          Emails.deliver(
            org = membership.organization,
            publication = Publication.MembershipsCreate,
            subject = s"${membership.organization.name}: ${membership.user.email} has joined as ${membership.role}",
            body = views.html.emails.membershipCreated(membership).toString
          )
        }
      }
    )

    case m: Any => {
      Logger.error("Email actor got an unhandled message: " + m)
    }

  }
}

