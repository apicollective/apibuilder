package db

import io.apibuilder.api.v0.models.{Organization, Publication}
import io.apibuilder.common.v0.models.MembershipRole
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class SubscriptionsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private lazy val org: InternalOrganization = createOrganization()

  "when a user loses admin role, we remove subscriptions that require admin" in {
    val user = createRandomUser()
    val membership = createMembership(org, user, MembershipRole.Admin)

    Publication.all.foreach { publication => createSubscription(org, user, publication) }

    membershipsDao.softDelete(testUser, membership)

    val subscriptions = subscriptionsDao.findAll(
      Authorization.All,
      organizationGuid = Some(org.guid),
      userGuid = Some(user.guid)
    ).map(_.publication)

    Publication.all.foreach { publication =>
      if (SubscriptionsDao.PublicationsRequiredAdmin.contains(publication)) {
        subscriptions.contains(publication) must be(false)
      } else {
        subscriptions.contains(publication) must be(true)
      }
    }

  }

}
