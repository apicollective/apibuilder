package actors

import db.MembershipsDao
import lib.Role
import io.apibuilder.apidoc.api.v0.models.Visibility
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
      membershipsDao.softDelete(user, membership)
      user
    }

    it("Context.Application for private app") {
      val app = db.Util.createApplication(org = org)
      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) should be(false)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) should be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) should be(false)
    }

    it("Context.Application for public app") {
      val app = db.Util.createApplication(
        org = org,
        db.Util.createApplicationForm(visibility = Visibility.Public)
      )

      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) should be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) should be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) should be(true)
    }

    it("Context.OrganizationAdmin") {
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, randomUser) should be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgMember) should be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgAdmin) should be(true)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, formerMember) should be(false)
    }

    it("Context.OrganizationMember") {
      emails.isAuthorized(Emails.Context.OrganizationMember, org, randomUser) should be(false)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgMember) should be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgAdmin) should be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, formerMember) should be(false)
    }

  }

}
