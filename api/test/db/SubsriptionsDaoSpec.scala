package db

import io.apibuilder.api.v0.models.Publication
import lib.Role
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class SubsriptionDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  lazy val org = createOrganization()

  "when a user loses admin role, we remove subscriptions that require admin" in {
    val user = createRandomUser()
    val membership = createMembership(org, user, Role.Admin)

    Publication.all.foreach { publication => createSubscription(org, user, publication) }

    membershipsDao.softDelete(testUser, membership)

    val subscriptions = subscriptionsDao.findAll(
      Authorization.All,
      organization = Some(org),
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
