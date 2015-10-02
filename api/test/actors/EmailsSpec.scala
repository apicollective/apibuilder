package actors

import db.MembershipsDao
import lib.Role
import com.bryzek.apidoc.api.v0.models.Visibility
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class EmailsSpec extends FunSpec with Matchers with util.TestApplication {

  describe("isAuthorized") {

    lazy val org = db.Util.createOrganization()
    lazy val randomUser = db.Util.createRandomUser()

    lazy val orgMember = {
      val user = db.Util.createRandomUser()
      db.Util.createMembership(org, user, Role.Member)
      user
    }

    lazy val orgAdmin = {
      val user = db.Util.createRandomUser()
      db.Util.createMembership(org, user, Role.Admin)
      user
    }

    lazy val formerMember = {
      val user = db.Util.createRandomUser()
      val membership = db.Util.createMembership(org, user)
      MembershipsDao.softDelete(user, membership)
      user
    }

    it("Context.Application for private app") {
      val app = db.Util.createApplication(org = org)
      Emails.isAuthorized(Emails.Context.Application(app), org, randomUser) should be(false)
      Emails.isAuthorized(Emails.Context.Application(app), org, orgMember) should be(true)
      Emails.isAuthorized(Emails.Context.Application(app), org, formerMember) should be(false)
    }

    it("Context.Application for public app") {
      val app = db.Util.createApplication(
        org = org,
        db.Util.createApplicationForm(visibility = Visibility.Public)
      )

      Emails.isAuthorized(Emails.Context.Application(app), org, randomUser) should be(true)
      Emails.isAuthorized(Emails.Context.Application(app), org, orgMember) should be(true)
      Emails.isAuthorized(Emails.Context.Application(app), org, formerMember) should be(true)
    }

    it("Context.OrganizationAdmin") {
      Emails.isAuthorized(Emails.Context.OrganizationAdmin, org, randomUser) should be(false)
      Emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgMember) should be(false)
      Emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgAdmin) should be(true)
      Emails.isAuthorized(Emails.Context.OrganizationAdmin, org, formerMember) should be(false)
    }

    it("Context.OrganizationMember") {
      Emails.isAuthorized(Emails.Context.OrganizationMember, org, randomUser) should be(false)
      Emails.isAuthorized(Emails.Context.OrganizationMember, org, orgMember) should be(true)
      Emails.isAuthorized(Emails.Context.OrganizationMember, org, orgAdmin) should be(true)
      Emails.isAuthorized(Emails.Context.OrganizationMember, org, formerMember) should be(false)
    }

  }

}
