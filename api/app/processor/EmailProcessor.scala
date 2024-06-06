package processor

import cats.data.ValidatedNec
import cats.implicits._
import db.{Authorization, InternalTasksDao, OrganizationsDao, UsersDao}
import io.apibuilder.api.v0.models.Publication
import io.apibuilder.task.v0.models._
import io.apibuilder.task.v0.models.json._
import lib.{AppConfig, EmailUtil, Person, Emails, Role}
import play.api.libs.json.Json

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

class EmailProcessorQueue @Inject() (
  internalTasksDao: InternalTasksDao
                                    ) {
  def queueWithConnection(c: Connection, data: EmailData): Unit = {
    val dataJson = Json.toJson(data)
    internalTasksDao.queue(TaskType.Email, id = Json.asciiStringify(dataJson), data = dataJson)
  }
}

class EmailProcessor @Inject()(
  args: TaskProcessorArgs,
  appConfig: AppConfig,
  applicationsDao: db.ApplicationsDao,
  email: EmailUtil,
  emails: Emails,
  emailVerificationsDao: db.EmailVerificationsDao,
  membershipsDao: db.MembershipsDao,
  membershipRequestsDao: db.MembershipRequestsDao,
  organizationsDao: OrganizationsDao,
  passwordResetRequestsDao: db.PasswordResetRequestsDao,
  usersDao: UsersDao,
) extends TaskProcessorWithData[EmailData](args, TaskType.Email) {

  override def processRecord(id: String, data: EmailData): ValidatedNec[String, Unit] = {
    data match {
      case EmailDataApplicationCreated(guid) => applicationCreated(guid).validNec
      case EmailDataEmailVerificationCreated(guid) => emailVerificationCreated(guid).validNec
      case EmailDataMembershipCreated(guid) => membershipCreated(guid).validNec
      case EmailDataMembershipRequestCreated(guid) => membershipRequestCreated(guid).validNec
      case EmailDataMembershipRequestAccepted(orgGuid, userGuid, role) => membershipRequestAccepted(orgGuid, userGuid, role)
      case EmailDataMembershipRequestDeclined(orgGuid, userGuid, role) => membershipRequestDeclined(orgGuid, userGuid, role)
      case EmailDataPasswordResetRequestCreated(guid) => passwordResetRequestCreated(guid).validNec
      case EmailDataUndefinedType(description) => s"Invalid email data type '$description'".invalidNec
    }
  }

  private[this] def applicationCreated(applicationGuid: UUID): Unit = {
    applicationsDao.findByGuid(Authorization.All, applicationGuid).foreach { application =>
      organizationsDao.findByGuid(Authorization.All, application.guid).foreach { org =>
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

  private[this] def emailVerificationCreated(guid: UUID): Unit = {
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

  private[this] def membershipCreated(guid: UUID): Unit = {
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

  private[this] def membershipRequestCreated(guid: UUID): Unit = {
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

  private[this] def validateRole(role: String): ValidatedNec[String, Role] = {
    Role.fromString(role).toValidNec(s"Invalid role '$role'")
  }

  private[this] def membershipRequestAccepted(orgGuid: UUID, userGuid: UUID, role: String): ValidatedNec[String, Unit] = {
    validateRole(role).map { vRole =>
      organizationsDao.findByGuid(Authorization.All, orgGuid).foreach { org =>
        usersDao.findByGuid(userGuid).foreach { user =>
          email.sendHtml(
            to = Person(user),
            subject = s"Welcome to ${org.name}",
            body = views.html.emails.membershipRequestAccepted(org, user, vRole).toString
          )
        }
      }
    }
  }

  private[this] def membershipRequestDeclined(orgGuid: UUID, userGuid: UUID, role: String): ValidatedNec[String, Unit] = {
    validateRole(role).map { vRole =>
      organizationsDao.findByGuid(Authorization.All, orgGuid).foreach { org =>
        usersDao.findByGuid(userGuid).foreach { user =>
          email.sendHtml(
            to = Person(user),
            subject = s"Your Membership Request to join ${org.name} was declined",
            body = views.html.emails.membershipRequestDeclined(org, user, vRole).toString
          )
        }
      }
    }
  }

  private[this] def passwordResetRequestCreated(guid: UUID): Unit = {
    passwordResetRequestsDao.findByGuid(guid).foreach { request =>
      usersDao.findByGuid(request.userGuid).foreach { user =>
        email.sendHtml(
          to = Person(user),
          subject = "Reset your password",
          body = views.html.emails.passwordResetRequestCreated(appConfig, request.token).toString
        )
      }
    }
  }
}