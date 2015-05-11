package actors

import com.gilt.apidoc.api.v0.models.{Application, Membership, Publication, Version}
import lib.{Difference, Email, Person, Role, ServiceDiff}
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
    case class VersionCreated(guid: UUID)
    case class VersionReplaced(oldGuid: UUID, newGuid: UUID)
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

    case EmailActor.Messages.VersionCreated(guid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.VersionCreated($guid)", {
        versionCreated(guid, oldVersion = None)
      }
    )

    case EmailActor.Messages.VersionReplaced(oldGuid, newGuid) => Util.withVerboseErrorHandler(
      s"EmailActor.Messages.VersionReplaced($oldGuid, $newGuid)", {
        VersionsDao.findByGuid(Authorization.All, oldGuid, isDeleted = None) match {
          case None => {
            versionCreated(newGuid, oldVersion = None)
          }
          case Some(oldVersion) => {
            versionCreated(newGuid, oldVersion = Some(oldVersion))
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

  private def versionCreated(
    guid: UUID,
    oldVersion: Option[Version]
  ) {
    VersionsDao.findByGuid(Authorization.All, guid).map { version =>
      ApplicationsDao.findAll(Authorization.All, version = Some(version), limit = 1).headOption.map { application =>
        OrganizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
          val diff = oldVersion match {
            case None => Nil
            case Some(old) => ServiceDiff(old.service, version.service).differences
          }

          Emails.deliver(
            org = org,
            publication = Publication.VersionsCreate,
            subject = s"${org.name}/${application.name}: New Version Uploaded (${version.version}) ",
            body = views.html.emails.versionCreated(
              org,
              application,
              version,
              breakingChanges = diff.filter { d =>
                d match {
                  case Difference.Breaking(desc) => true
                  case Difference.NonBreaking(desc) => false
                }
              },
              nonBreakingChanges = diff.filter { d =>
                d match {
                  case Difference.Breaking(desc) => false
                  case Difference.NonBreaking(desc) => true
                }
              }
            ).toString
          )
        }
      }
    }
  }

}

