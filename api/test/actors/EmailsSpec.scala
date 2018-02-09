package actors

import db.MembershipsDao
import lib.Role
import io.apibuilder.api.v0.models.Visibility
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class EmailsSpec extends PlaySpec with OneAppPerSuite with util.Daos {

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
      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) must be(false)
    }

    it("Context.Application for public app") {
      val app = db.Util.createApplication(
        org = org,
        db.Util.createApplicationForm(visibility = Visibility.Public)
      )

      emails.isAuthorized(Emails.Context.Application(app), org, randomUser) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.Application(app), org, formerMember) must be(true)
    }

    it("Context.OrganizationAdmin") {
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgMember) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, orgAdmin) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationAdmin, org, formerMember) must be(false)
    }

    it("Context.OrganizationMember") {
      emails.isAuthorized(Emails.Context.OrganizationMember, org, randomUser) must be(false)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgMember) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, orgAdmin) must be(true)
      emails.isAuthorized(Emails.Context.OrganizationMember, org, formerMember) must be(false)
    }

  }

}
