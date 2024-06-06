package lib

import io.apibuilder.api.v0.models.Visibility
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class EmailsSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  "isAuthorized" must {

    lazy val org = createOrganization()
    lazy val randomUser = createRandomUser()

    lazy val orgMember = {
      val user = createRandomUser()
      createMembership(org, user, Role.Member)
      user
    }

    lazy val orgAdmin = {
      val user = createRandomUser()
      createMembership(org, user, Role.Admin)
      user
    }

    lazy val formerMember = {
      val user = createRandomUser()
      val membership = createMembership(org, user)
      membershipsDao.softDelete(user, membership)
      user
    }

    "Context.Application for private app" in {
      val app = createApplication(org = org)
      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) must be(false)
    }

    "Context.Application for public app" in {
      val app = createApplication(
        org = org,
        createApplicationForm(visibility = Visibility.Public)
      )

      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) must be(true)
    }

    "Context.OrganizationAdmin" in {
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgMember) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgAdmin) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, formerMember) must be(false)
    }

    "Context.OrganizationMember" in {
      emails.isAuthorized(Emails.Context.OrganizationMember, org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgAdmin) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, formerMember) must be(false)
    }

  }

}
