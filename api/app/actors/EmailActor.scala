package actors

import com.gilt.apidoc.models.{Membership, Publication, Service}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import db._
import lib.{Email, Pager, Person, Role}
import akka.actor._
import play.api.Logger
import play.api.Play.current
import java.util.UUID

object EmailActor {

  object Messages {
    case class MembershipRequestCreated(guid: UUID)
    case class MembershipCreated(guid: UUID)
    case class ServiceCreated(guid: UUID)
    case class VersionCreated(guid: UUID)
    case class EmailVerificationCreated(guid: UUID)
  }

}

class EmailActor extends Actor {

  def receive = {

    case EmailActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipRequestCreated($guid)", {
        MembershipRequestsDao.findByGuid(Authorization.All, guid).map { request =>
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
        MembershipsDao.findByGuid(Authorization.All, guid).map { membership =>
          Emails.deliver(
            org = membership.organization,
            publication = Publication.MembershipsCreate,
            subject = s"${membership.organization.name}: ${membership.user.email} has joined as ${membership.role}",
            body = views.html.emails.membershipCreated(membership).toString
          )
        }
      }
    )

    case EmailActor.Messages.ServiceCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.ServiceCreated($guid)", {
        ServicesDao.findByGuid(Authorization.All, guid).map { service =>
          OrganizationsDao.findAll(Authorization.All, service = Some(service)).map { org =>
            Emails.deliver(
              org = org,
              publication = Publication.ServicesCreate,
              subject = s"${org.name}: New Service Created - ${service.name}",
              body = views.html.emails.serviceCreated(org, service).toString
            )
          }
        }
      }
    )

    case EmailActor.Messages.VersionCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.ServiceCreated($guid)", {
        VersionsDao.findByGuid(Authorization.All, guid).map { version =>
          ServicesDao.findAll(Authorization.All, version = Some(version), limit = 1).headOption.map { service =>
            OrganizationsDao.findAll(Authorization.All, service = Some(service), limit = 1).headOption.map { org =>
              Emails.deliver(
                org = org,
                publication = Publication.VersionsCreate,
                subject = s"${org.name}/${service.name}: New Version Uploaded (${version.version}) ",
                body = views.html.emails.versionCreated(org, service, version).toString
              )
            }
          }
        }
      }
    )

    case EmailActor.Messages.EmailVerificationCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.ServiceCreated($guid)", {
        EmailVerificationsDao.findByGuid(guid).map { verification =>
          UsersDao.findByGuid(verification.userGuid).map { user =>
            Email.sendHtml(
              to = Person(email = user.email, name = user.name),
              subject = s"Verify your email address",
              body = views.html.emails.emailVerificationCreated(verification).toString
            )
          }
        }
      }
    )

    case m: Any => {
      Logger.error("Email actor got an unhandled message: " + m)
    }

  }
}

