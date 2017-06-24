package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import db._
import io.apibuilder.apidoc.api.v0.models.{Application, Membership, Publication, Version}
import lib.{Email, Person, Role}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object EmailActor {

  object Messages {
    case class EmailVerificationCreated(guid: UUID)
    case class MembershipCreated(guid: UUID)
    case class MembershipRequestCreated(guid: UUID)
    case class MembershipRequestAccepted(organizationGuid: UUID, userGuid: UUID, role: Role)
    case class MembershipRequestDeclined(organizationGuid: UUID, userGuid: UUID, role: Role)
    case class PasswordResetRequestCreated(guid: UUID)
    case class ApplicationCreated(guid: UUID)
  }

}

@javax.inject.Singleton
class EmailActor @javax.inject.Inject() (
  system: ActorSystem,
  applicationsDao: db.ApplicationsDao,
  emails: Emails,
  emailVerificationsDao: db.EmailVerificationsDao,
  membershipsDao: db.MembershipsDao,
  membershipRequestsDao: db.MembershipRequestsDao,
  organizationsDao: OrganizationsDao,
  passwordResetRequestsDao: db.PasswordResetRequestsDao,
  usersDao: UsersDao
) extends Actor with ActorLogging with ErrorHandler {

  implicit val ec = system.dispatchers.lookup("email-actor-context")

  def receive = {

    case m @ EmailActor.Messages.MembershipRequestCreated(guid) => withVerboseErrorHandler(m) {
      membershipRequestsDao.findByGuid(Authorization.All, guid).map { request =>
        emails.deliver(
          context = Emails.Context.OrganizationAdmin,
          org = request.organization,
          publication = Publication.MembershipRequestsCreate,
          subject = s"${request.organization.name}: Membership Request from ${request.user.email}",
          body = views.html.emails.membershipRequestCreated(request).toString
        )
      }
    }

    case m @ EmailActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      organizationsDao.findByGuid(Authorization.All, organizationGuid).map { org =>
        usersDao.findByGuid(userGuid).map { user =>
          Email.sendHtml(
            to = Person(user),
            subject = s"Welcome to ${org.name}",
            body = views.html.emails.membershipRequestAccepted(org, user, role).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      organizationsDao.findByGuid(Authorization.All, organizationGuid).map { org =>
        usersDao.findByGuid(userGuid).map { user =>
          Email.sendHtml(
            to = Person(user),
            subject = s"Your Membership Request to join ${org.name} was declined",
            body = views.html.emails.membershipRequestDeclined(org, user, role).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.MembershipCreated(guid) => withVerboseErrorHandler(m) {
      membershipsDao.findByGuid(Authorization.All, guid).map { membership =>
        emails.deliver(
          context = Emails.Context.OrganizationAdmin,
          org = membership.organization,
          publication = Publication.MembershipsCreate,
          subject = s"${membership.organization.name}: ${membership.user.email} has joined as ${membership.role}",
          body = views.html.emails.membershipCreated(membership).toString
        )
      }
    }

    case m @ EmailActor.Messages.ApplicationCreated(guid) => withVerboseErrorHandler(m) {
      applicationsDao.findByGuid(Authorization.All, guid).map { application =>
        organizationsDao.findAll(Authorization.All, application = Some(application)).map { org =>
          emails.deliver(
            context = Emails.Context.OrganizationMember,
            org = org,
            publication = Publication.ApplicationsCreate,
            subject = s"${org.name}: New Application Created - ${application.name}",
            body = views.html.emails.applicationCreated(org, application).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.PasswordResetRequestCreated(guid) => withVerboseErrorHandler(m) {
      passwordResetRequestsDao.findByGuid(guid).map { request =>
        usersDao.findByGuid(request.userGuid).map { user =>
          Email.sendHtml(
            to = Person(user),
            subject = s"Reset your password",
            body = views.html.emails.passwordResetRequestCreated(request.token).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.EmailVerificationCreated(guid) => withVerboseErrorHandler(m) {
      emailVerificationsDao.findByGuid(guid).map { verification =>
        usersDao.findByGuid(verification.userGuid).map { user =>
          Email.sendHtml(
            to = Person(email = verification.email, name = user.name),
            subject = s"Verify your email address",
            body = views.html.emails.emailVerificationCreated(verification).toString
          )
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

