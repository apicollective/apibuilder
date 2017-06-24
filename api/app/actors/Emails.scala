package actors

import akka.actor._
import io.apibuilder.api.v0.models.{Application, Organization, Publication, Subscription, User, Visibility}
import db.{ApplicationsDao, Authorization, MembershipsDao, SubscriptionsDao}
import javax.inject.{Inject, Singleton}
import lib.{Config, Email, Pager, Person}
import play.api.Logger
import play.api.Play.current

object Emails {

  /**
    * Context is used to enforce permissions - only delivering email
    * when the user in fact has access to the specified resource. For
    * example, if the email is being sent regarding an update to an
    * application context, we will ensure that the user can actually
    * view that application prior to sending the update. This allows
    * users to be subscribed to updates for public applications while
    * never receiving communciation for non public applications (as an
    * example).
    */
  sealed trait Context
  object Context {
    case class Application(application: io.apibuilder.api.v0.models.Application) extends Context
    case object OrganizationAdmin extends Context
    case object OrganizationMember extends Context
  }

}

@Singleton
class Emails @Inject() (
  applicationsDao: ApplicationsDao,
  membershipsDao: MembershipsDao,
  subscriptionsDao: SubscriptionsDao
) {

  private lazy val sendErrorsTo = Config.requiredString("apibuilder.sendErrorsTo").split("\\s+")

  def deliver(
    context: Emails.Context,
    org: Organization,
    publication: Publication,
    subject: String,
    body: String
  ) (
    implicit filter: Subscription => Boolean = { _ => true }
  ) {
    eachSubscription(context, org, publication, { subscription =>
      Email.sendHtml(
        to = Person(subscription.user),
        subject = subject,
        body = body
      )
    })
  }

  private[this] def eachSubscription(
    context: Emails.Context,
    organization: Organization,
    publication: Publication,
    f: Subscription => Unit
  ) {
    Pager.eachPage[Subscription] { offset =>
      subscriptionsDao.findAll(
        Authorization.All,
        organization = Some(organization),
        publication = Some(publication),
        limit = 100,
        offset = offset
      )
    } { subscription =>
      isAuthorized(context, organization, subscription.user) match {
        case false => {
          Logger.info(s"Emails: publication[$publication] subscription[$subscription] - not authorized for context[$context]. Skipping email")
        }
        case true => {
          Logger.info(s"Emails: delivering email for publication[$publication] subscription[$subscription]")
          f(subscription)
        }
      }
    }
  }

  private[actors] def isAuthorized(
    context: Emails.Context,
    organization: Organization,
    user: User
  ): Boolean = {
    context match {
      case Emails.Context.Application(app) => {
        app.visibility match {
          case Visibility.Public => true
          case Visibility.User | Visibility.Organization => {
            applicationsDao.findByGuid(Authorization.User(user.guid), app.guid) match {
              case None => false
              case Some(_) => true
            }
          }
          case Visibility.UNDEFINED(name) => {
            Logger.warn(s"Undefined visibility[$name] -- default behaviour assumes NOT AUTHORIZED")
            false
          }
        }
      }
      case Emails.Context.OrganizationAdmin => {
        membershipsDao.isUserAdmin(user, organization)
      }
      case Emails.Context.OrganizationMember => {
        membershipsDao.isUserMember(user, organization)
      }
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
