package actors

import akka.actor.{Actor, ActorLogging}
import db._
import io.apibuilder.api.v0.models.Publication
import lib.{AppConfig, EmailUtil, Person, Role}

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
  appConfig: AppConfig,
  applicationsDao: db.ApplicationsDao,
  email: EmailUtil,
  emails: Emails,
  emailVerificationsDao: db.EmailVerificationsDao,
  membershipsDao: db.MembershipsDao,
  membershipRequestsDao: db.MembershipRequestsDao,
  organizationsDao: OrganizationsDao,
  passwordResetRequestsDao: db.PasswordResetRequestsDao,
  usersDao: UsersDao
) extends Actor with ActorLogging with ErrorHandler {

  def receive: Receive = {

    case m @ EmailActor.Messages.MembershipRequestCreated(guid) => withVerboseErrorHandler(m) {
      membershipRequestsDao.findByGuid(Authorization.All, guid).foreach { request =>
        emails.deliver(
          context = Emails.Context.OrganizationAdmin,
          org = request.organization,
          publication = Publication.MembershipRequestsCreate,
          subject = s"${request.organization.name}: Membership Request from ${request.user.email}",
          body = views.html.emails.membershipRequestCreated(appConfig, request).toString
        )
      }
    }

    case m @ EmailActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      organizationsDao.findByGuid(Authorization.All, organizationGuid).foreach { org =>
        usersDao.findByGuid(userGuid).foreach { user =>
          email.sendHtml(
            to = Person(user),
            subject = s"Welcome to ${org.name}",
            body = views.html.emails.membershipRequestAccepted(org, user, role).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role) => withVerboseErrorHandler(m) {
      organizationsDao.findByGuid(Authorization.All, organizationGuid).foreach { org =>
        usersDao.findByGuid(userGuid).foreach { user =>
          email.sendHtml(
            to = Person(user),
            subject = s"Your Membership Request to join ${org.name} was declined",
            body = views.html.emails.membershipRequestDeclined(org, user, role).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.MembershipCreated(guid) => withVerboseErrorHandler(m) {
      membershipsDao.findByGuid(Authorization.All, guid).foreach { membership =>
        emails.deliver(
          context = Emails.Context.OrganizationAdmin,
          org = membership.organization,
          publication = Publication.MembershipsCreate,
          subject = s"${membership.organization.name}: ${membership.user.email} has joined as ${membership.role}",
          body = views.html.emails.membershipCreated(appConfig, membership).toString
        )
      }
    }

    case m @ EmailActor.Messages.ApplicationCreated(guid) => withVerboseErrorHandler(m) {
      applicationsDao.findByGuid(Authorization.All, guid).foreach { application =>
        organizationsDao.findAll(Authorization.All, application = Some(application)).foreach { org =>
          emails.deliver(
            context = Emails.Context.OrganizationMember,
            org = org,
            publication = Publication.ApplicationsCreate,
            subject = s"${org.name}: New Application Created - ${application.name}",
            body = views.html.emails.applicationCreated(appConfig, org, application).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.PasswordResetRequestCreated(guid) => withVerboseErrorHandler(m) {
      passwordResetRequestsDao.findByGuid(guid).foreach { request =>
        usersDao.findByGuid(request.userGuid).foreach { user =>
          email.sendHtml(
            to = Person(user),
            subject = s"Reset your password",
            body = views.html.emails.passwordResetRequestCreated(appConfig, request.token).toString
          )
        }
      }
    }

    case m @ EmailActor.Messages.EmailVerificationCreated(guid) => withVerboseErrorHandler(m) {
      emailVerificationsDao.findByGuid(guid).foreach { verification =>
        usersDao.findByGuid(verification.userGuid).foreach { user =>
          email.sendHtml(
            to = Person(email = verification.email, name = user.name),
            subject = "Verify your email address",
            body = views.html.emails.emailVerificationCreated(appConfig, verification).toString
          )
        }
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

}

