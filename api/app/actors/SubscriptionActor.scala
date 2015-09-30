package actors

import com.bryzek.apidoc.api.v0.models.Subscription
import db.{Authorization, SubscriptionsDao, UsersDao}
import lib.Pager
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object SubscriptionActor {

  object Messages {
    case class MembershipDeleted(organizationGuid: UUID, userGuid: UUID)
  }

}

class SubscriptionActor extends Actor {

  private[this] val NumberDaysBeforePurge = 90

  def receive = {

    // TODO: This is a bit tricky as the user might be replacing the
    // member membership record with an 'admin' role. TODO: Think
    // about this.
    case SubscriptionActor.Messages.MembershipDeleted(organizationGuid, userGuid) => Util.withVerboseErrorHandler(
      s"SubscriptionActor.Messages.MembershipDeleted($organizationGuid, $userGuid)", {
        Pager.eachPage[Subscription] { offset =>
          SubscriptionsDao.findAll(
            Authorization.All,
            organizationGuid = Some(organizationGuid),
            userGuid = Some(userGuid),
            offset = offset
          )
        } { subscription =>
          Logger.info(s"Deleting subscription[${subscription.guid}] as membership for user[$userGuid] in organization[$organizationGuid] has ended")
          SubscriptionsDao.softDelete(UsersDao.AdminUser, subscription)
        }
      }
    )

  }

}
