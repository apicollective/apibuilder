package actors

import com.bryzek.apidoc.api.v0.models.{Application, Membership, Publication, Version}
import lib.{Email, Person, Role}
import db._
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

class EmailActor extends Actor {

  def receive = {

    case EmailActor.Messages.MembershipRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipRequestCreated($guid)", {
        MembershipRequestsDao.findByGuid(Authorization.All, guid).map { request =>
          Emails.deliver(
            context = Emails.Context.OrganizationAdmin,
            org = request.organization,
            publication = Publication.MembershipRequestsCreate,
            subject = s"${request.organization.name}: Membership Request from ${request.user.email}",
            body = views.html.emails.membershipRequestCreated(request).toString
          )
        }
      }
    )

    case EmailActor.Messages.MembershipRequestAccepted(organizationGuid, userGuid, role) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipRequestAccepted($organizationGuid, $userGuid, $role)", {
        OrganizationsDao.findByGuid(Authorization.All, organizationGuid).map { org =>
          UsersDao.findByGuid(userGuid).map { user =>
            Email.sendHtml(
              to = Person(user),
              subject = s"Welcome to ${org.name}",
              body = views.html.emails.membershipRequestAccepted(org, user, role).toString
            )
          }
        }
      }
    )

    case EmailActor.Messages.MembershipRequestDeclined(organizationGuid, userGuid, role) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipRequestDeclined($organizationGuid, $userGuid, $role)", {
        OrganizationsDao.findByGuid(Authorization.All, organizationGuid).map { org =>
          UsersDao.findByGuid(userGuid).map { user =>
            Email.sendHtml(
              to = Person(user),
              subject = s"Your Membership Request to join ${org.name} was declined",
              body = views.html.emails.membershipRequestDeclined(org, user, role).toString
            )
          }
        }
      }
    )

    case EmailActor.Messages.MembershipCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.MembershipCreated($guid)", {
        MembershipsDao.findByGuid(Authorization.All, guid).map { membership =>
          Emails.deliver(
            context = Emails.Context.OrganizationAdmin,
            org = membership.organization,
            publication = Publication.MembershipsCreate,
            subject = s"${membership.organization.name}: ${membership.user.email} has joined as ${membership.role}",
            body = views.html.emails.membershipCreated(membership).toString
          )
        }
      }
    )

    case EmailActor.Messages.ApplicationCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.ApplicationCreated($guid)", {
        ApplicationsDao.findByGuid(Authorization.All, guid).map { application =>
          OrganizationsDao.findAll(Authorization.All, application = Some(application)).map { org =>
            Emails.deliver(
              context = Emails.Context.OrganizationMember,
              org = org,
              publication = Publication.ApplicationsCreate,
              subject = s"${org.name}: New Application Created - ${application.name}",
              body = views.html.emails.applicationCreated(org, application).toString
            )
          }
        }
      }
    )

    case EmailActor.Messages.PasswordResetRequestCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.PasswordResetRequestCreated($guid)", {
        PasswordResetRequestsDao.findByGuid(guid).map { request =>
          UsersDao.findByGuid(request.userGuid).map { user =>
            Email.sendHtml(
              to = Person(user),
              subject = s"Reset your password",
              body = views.html.emails.passwordResetRequestCreated(request.token).toString
            )
          }
        }
      }
    )

    case EmailActor.Messages.EmailVerificationCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.EmailVerificationCreated($guid)", {
        EmailVerificationsDao.findByGuid(guid).map { verification =>
          UsersDao.findByGuid(verification.userGuid).map { user =>
            Email.sendHtml(
              to = Person(email = verification.email, name = user.name),
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

