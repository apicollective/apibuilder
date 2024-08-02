package lib

import db.InternalUser
import io.apibuilder.api.v0.models.Visibility
import io.apibuilder.common.v0.models.MembershipRole
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EmailsSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  "isAuthorized" must {

    lazy val org = createOrganization()
    lazy val randomUser = createRandomUser()

    lazy val orgMember = {
      val user = createRandomUser()
      createMembership(org, user, MembershipRole.Member)
      user
    }

    lazy val orgAdmin = {
      val user = createRandomUser()
      createMembership(org, user, MembershipRole.Admin)
      user
    }

    lazy val formerMember = {
      val user = createRandomUser()
      val membership = createMembership(org, user)
      membershipsDao.softDelete(user.reference, membership)
      user
    }

    "Context.Application for private app" in {
      val app = createApplication(org = org)
      def check(user: InternalUser): Boolean = {
        emails.isAuthorized(Emails.Context.Application(app), org.reference, user.reference)
      }

      check(randomUser) must be(false)
      check(orgMember) must be(true)
      check(formerMember) must be(false)
    }

    "Context.Application for public app" in {
      val app = createApplication(
        org = org,
        createApplicationForm(visibility = Visibility.Public)
      )

      def check(user: InternalUser): Boolean = {
        emails.isAuthorized(Emails.Context.Application(app), org.reference, user.reference)
      }

      check(randomUser) must be(true)
      check(orgMember) must be(true)
      check(formerMember) must be(true)
    }

    "Context.OrganizationAdmin" in {
      def check(user: InternalUser): Boolean = {
        emails.isAuthorized(Emails.Context.OrganizationAdmin, org.reference, user.reference)
      }

      check(randomUser) must be(false)
      check(orgMember) must be(false)
      check(orgAdmin) must be(true)
      check(formerMember) must be(false)
    }

    "Context.OrganizationMember" in {
      def check(user: InternalUser): Boolean = {
        emails.isAuthorized(Emails.Context.OrganizationMember, org.reference, user.reference)
      }

      check(randomUser) must be(false)
      check(orgMember) must be(true)
      check(orgAdmin) must be(true)
      check(formerMember) must be(false)
    }

  }

}
