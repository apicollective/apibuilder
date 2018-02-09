package db

import io.apibuilder.api.v0.models.{Organization, Publication}
import lib.Role
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class SubsriptionDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  lazy val org = Util.createOrganization()

  "when a user loses admin role, we remove subscriptions that require admin" in {
    val user = Util.createRandomUser()
    val membership = Util.createMembership(org, user, Role.Admin)

    Publication.all.foreach { publication => Util.createSubscription(org, user, publication) }

    membershipsDao.softDelete(Util.createdBy, membership)

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
