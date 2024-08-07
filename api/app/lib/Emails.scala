package lib

import db.*
import io.apibuilder.api.v0.models.*
import models.SubscriptionModel
import play.api.Logging

import java.util.UUID
import javax.inject.{Inject, Singleton}

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
    case class Application(application: InternalApplication) extends Context
    case object OrganizationAdmin extends Context
    case object OrganizationMember extends Context
  }

}

@Singleton
class Emails @Inject() (
                         email: EmailUtil,
                         applicationsDao: InternalApplicationsDao,
                         membershipsDao: InternalMembershipsDao,
                         subscriptionsDao: InternalSubscriptionsDao,
                         subscriptionModel: SubscriptionModel,
) extends Logging {

  def deliver(
               context: Emails.Context,
               org: Organization,
               publication: Publication,
               subject: String,
               body: String
             ) (
               implicit filter: Subscription => Boolean = { _ => true }
             ): Unit = {

    eachSubscription(context, OrganizationReference(org), publication, { subscription =>
      val result = filter(subscription) // TODO: Should we use filter?
      email.sendHtml(
        to = Person(subscription.user),
        subject = subject,
        body = body
      )
    })
  }

  private def eachSubscription(
                                context: Emails.Context,
                                organization: OrganizationReference,
                                publication: Publication,
                                f: Subscription => Unit
  ): Unit = {
    Pager.eachPage[Subscription] { offset =>
      subscriptionModel.toModels(
        subscriptionsDao.findAll(
          Authorization.All,
          organizationGuid = Some(organization.guid),
          publication = Some(publication),
          limit = Some(100),
          offset = offset
        )
      )
    } { subscription =>
      if (isAuthorized(context, organization, UserReference(subscription.user))) {
        logger.info(s"Emails: delivering email for publication[$publication] subscription[$subscription]")
        f(subscription)
      } else {
        logger.info(s"Emails: publication[$publication] subscription[$subscription] - not authorized for context[$context]. Skipping email")
      }
    }
  }

  private[lib] def isAuthorized(
    context: Emails.Context,
    organization: OrganizationReference,
    user: UserReference
  ): Boolean = {
    isAuthorized(
      context, organizationGuid = organization.guid, userGuid = user.guid
    )
  }

  private[lib] def isAuthorized(
                                 context: Emails.Context,
                                 organizationGuid: UUID,
                                 userGuid: UUID
                               ): Boolean = {

    context match {
      case Emails.Context.Application(app) => {
        app.visibility match {
          case Visibility.Public => true
          case Visibility.User | Visibility.Organization => {
            applicationsDao.findByGuid(Authorization.User(userGuid), app.guid) match {
              case None => false
              case Some(_) => true
            }
          }
          case Visibility.UNDEFINED(name) => {
            logger.warn(s"Undefined visibility[$name] -- default behaviour assumes NOT AUTHORIZED")
            false
          }
        }
      }
      case Emails.Context.OrganizationAdmin => {
        membershipsDao.isUserAdmin(userGuid = userGuid, organizationGuid = organizationGuid)
      }
      case Emails.Context.OrganizationMember => {
        membershipsDao.isUserMember(userGuid = userGuid, organizationGuid = organizationGuid)
      }
    }
  }

}
