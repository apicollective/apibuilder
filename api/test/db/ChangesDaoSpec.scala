package db

import com.bryzek.apidoc.api.v0.models.{Application, Change, Diff, DiffBreaking, DiffNonBreaking, Organization, Version}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ChangesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  private[this] def getApplication(version: Version): Application = {
    ApplicationsDao.findByGuid(Authorization.All, version.application.guid).getOrElse {
      sys.error("Could not find application for version: " + version)
    }
  }

  private[this] def createChange(
    description: String = "Breaking difference - " + UUID.randomUUID.toString,
    org: Organization = Util.createOrganization()
  ): Change = {
    val app = Util.createApplication(org = org)
    val fromVersion = Util.createVersion(application = app, version = "1.0.0")
    val toVersion = Util.createVersion(application = app, version = "1.0.1")
    val diff = DiffBreaking(description)
    ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))

    ChangesDao.findAll(
      Authorization.All,
      fromVersionGuid = Some(fromVersion.guid)
    ).head
  }

  describe("with a valid from and to version") {

    it("upsert is a no-op with no differences") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Nil)
      ChangesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ) should be(Nil)
    }

    it("upsert can record multiple changes") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val breaking = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      val nonBreaking = DiffNonBreaking("Non breaking difference - " + UUID.randomUUID.toString)

      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(breaking, nonBreaking))

      val actual = ChangesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff)

      actual.size should be(2)
      actual.contains(breaking) should be(true)
      actual.contains(nonBreaking) should be(true)
    }

    it("upsert does not throw error on duplicate") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val diff = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff, diff))
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))

      ChangesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff) should be(Seq(diff))
    }

  }

  it("upsert raises errors if applications are different") {
    val fromVersion = Util.createVersion(version = "1.0.0")
    val toVersion = Util.createVersion(version = "1.0.1")

    intercept[AssertionError] {
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Nil)
    }.getMessage should be("assertion failed: Versions must belong to same application")
  }

  describe("findAll") {

    it("guid") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, guid = Some(change.guid)).map(_.guid) should be(Seq(change.guid))
    }

    it("organizationGuid") {
      val org = Util.createOrganization()
      val change = createChange(org = org)
      ChangesDao.findAll(Authorization.All, organizationGuid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, organizationGuid = Some(org.guid)).map(_.guid) should be(Seq(change.guid))
    }

    it("organizationKey") {
      val org = Util.createOrganization()
      val change = createChange(org = org)
      ChangesDao.findAll(Authorization.All, organizationKey = Some(UUID.randomUUID.toString)) should be(Nil)
      ChangesDao.findAll(Authorization.All, organizationKey = Some(org.key)).map(_.guid) should be(Seq(change.guid))
    }

    it("applicationGuid") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, applicationGuid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, applicationGuid = Some(change.application.guid)).map(_.guid) should be(Seq(change.guid))
    }

    it("applicationKey") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, applicationKey = Some(UUID.randomUUID.toString)) should be(Nil)
      ChangesDao.findAll(Authorization.All, applicationKey = Some(change.application.key)).map(_.guid) should be(Seq(change.guid))
    }

    it("fromVersionGuid") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, fromVersionGuid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, fromVersionGuid = Some(change.fromVersion.guid)).map(_.guid) should be(Seq(change.guid))
    }

    it("toVersionGuid") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, toVersionGuid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, toVersionGuid = Some(change.toVersion.guid)).map(_.guid) should be(Seq(change.guid))
    }

    it("description") {
      val desc = UUID.randomUUID.toString
      val change = createChange(description = desc)
      ChangesDao.findAll(Authorization.All, description = Some(UUID.randomUUID.toString)) should be(Nil)
      ChangesDao.findAll(Authorization.All, description = Some(desc)).map(_.guid) should be(Seq(change.guid))
    }

    it("limit and offset") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1).map(_.guid) should be(Seq(change.guid))
      ChangesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1, offset = 1).map(_.guid) should be(Nil)
    }

    it("authorization") {
      val change = createChange()

      val user = Util.createRandomUser()
      ChangesDao.findAll(Authorization.User(user.guid), guid = Some(change.guid)).map(_.guid) should be(Nil)
      ChangesDao.findAll(Authorization.PublicOnly, guid = Some(change.guid)).map(_.guid) should be(Nil)

      val app = ApplicationsDao.findByGuid(Authorization.All, change.application.guid).get
      Util.createMembership(
        OrganizationsDao.findByGuid(
          Authorization.All,
          app.organization.guid
        ).get,
        user
      )
      ChangesDao.findAll(Authorization.User(Util.createdBy.guid), guid = Some(change.guid)).map(_.guid) should be(Seq(change.guid))
    }
  }

}
