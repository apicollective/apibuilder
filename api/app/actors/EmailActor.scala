package actors

import com.gilt.apidoc.models.Publication
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import db.MembershipRequestDao
import lib.{Email, Pager, Person}
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object EmailActor {

  object Messages {
    case class MembershipRequestCreated(guid: UUID)
  }

}

class EmailActor extends Actor {

  def receive = {

    case MainActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"MainActor.Messages.MembershipRequestCreated($guid)", {
        MembershipRequestDao.findByGuid(guid).map { request =>
          Emails.deliver(
            org = request.organization,
            publication = Publication.MembershipRequestsCreate,
            subject = s"${request.organization.key}: Membership Request from ${request.user.email}",
            body = views.html.emails.membershipRequestCreated(request).toString
          )
        }
      }
    )

    case m: Any => {
      Logger.error("Main actor got an unhandled message: " + m)
    }

  }
}

