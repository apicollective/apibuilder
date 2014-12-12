package db

import com.gilt.apidoc.models.{Organization, Publication}
import lib.Role
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class SubsriptionDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  lazy val org = Util.createOrganization()

  it("when a user loses admin role, we remove subscriptions that require admin") {
    val user = Util.createRandomUser()
    val membership = Util.createMembership(org, user, Role.Admin)

    Publication.all.foreach { publication => Util.createSubscription(org, user, publication) }

    Membership.softDelete(Util.createdBy, membership)

    val subscriptions = SubscriptionDao.findAll(
      Authorization.All,
      organization = Some(org),
      userGuid = Some(user.guid)
    ).map(_.publication)

    Publication.all.foreach { publication =>
      if (SubscriptionDao.PublicationsRequiredAdmin.contains(publication)) {
        subscriptions.contains(publication) should be(false)
      } else {
        subscriptions.contains(publication) should be(true)
      }
    }

  }

}
