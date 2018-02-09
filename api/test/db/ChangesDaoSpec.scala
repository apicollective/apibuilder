package db

import io.apibuilder.api.v0.models.{Application, Change, Diff, DiffBreaking, DiffNonBreaking, Organization, Version}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class ChangesDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  private[this] def getApplication(version: Version): Application = {
    applicationsDao.findByGuid(Authorization.All, version.application.guid).getOrElse {
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
    changesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))

    changesDao.findAll(
      Authorization.All,
      fromVersionGuid = Some(fromVersion.guid)
    ).head
  }

  describe("with a valid from and to version") {

    it("upsert is a no-op with no differences") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      changesDao.upsert(Util.createdBy, fromVersion, toVersion, Nil)
      changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ) must be(Nil)
    }

    it("upsert can record multiple changes") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val breaking = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      val nonBreaking = DiffNonBreaking("Non breaking difference - " + UUID.randomUUID.toString)

      changesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(breaking, nonBreaking))

      val actual = changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff)

      actual.size must be(2)
      actual.contains(breaking) must be(true)
      actual.contains(nonBreaking) must be(true)
    }

    it("upsert does not throw error on duplicate") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val diff = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      changesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff, diff))
      changesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))

      changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff) must be(Seq(diff))
    }

  }

  it("upsert raises errors if applications are different") {
    val fromVersion = Util.createVersion(version = "1.0.0")
    val toVersion = Util.createVersion(version = "1.0.1")

    intercept[AssertionError] {
      changesDao.upsert(Util.createdBy, fromVersion, toVersion, Nil)
    }.getMessage must be("assertion failed: Versions must belong to same application")
  }

  describe("findAll") {

    it("guid") {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, guid = Some(change.guid)).map(_.guid) must be(Seq(change.guid))
    }

    it("organizationGuid") {
      val org = Util.createOrganization()
      val change = createChange(org = org)
      changesDao.findAll(Authorization.All, organizationGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, organizationGuid = Some(org.guid)).map(_.guid) must be(Seq(change.guid))
    }

    it("organizationKey") {
      val org = Util.createOrganization()
      val change = createChange(org = org)
      changesDao.findAll(Authorization.All, organizationKey = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, organizationKey = Some(org.key)).map(_.guid) must be(Seq(change.guid))
    }

    it("applicationGuid") {
      val change = createChange()
      changesDao.findAll(Authorization.All, applicationGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, applicationGuid = Some(change.application.guid)).map(_.guid) must be(Seq(change.guid))
    }

    it("applicationKey") {
      val change = createChange()
      changesDao.findAll(Authorization.All, applicationKey = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, applicationKey = Some(change.application.key)).map(_.guid) must be(Seq(change.guid))
    }

    it("fromVersionGuid") {
      val change = createChange()
      changesDao.findAll(Authorization.All, fromVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, fromVersionGuid = Some(change.fromVersion.guid)).map(_.guid) must be(Seq(change.guid))
    }

    it("toVersionGuid") {
      val change = createChange()
      changesDao.findAll(Authorization.All, toVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, toVersionGuid = Some(change.toVersion.guid)).map(_.guid) must be(Seq(change.guid))
    }

    it("fromVersion") {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(change.guid), fromVersion = Some(change.fromVersion.version)).map(_.guid) must be(Seq(change.guid))
    }

    it("toVersion") {
      val change = createChange()
      changesDao.findAll(Authorization.All, toVersion = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, guid = Some(change.guid), toVersion = Some(change.toVersion.version)).map(_.guid) must be(Seq(change.guid))
    }

    it("description") {
      val desc = UUID.randomUUID.toString
      val change = createChange(description = desc)
      changesDao.findAll(Authorization.All, description = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, description = Some(desc)).map(_.guid) must be(Seq(change.guid))
    }

    it("limit and offset") {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1).map(_.guid) must be(Seq(change.guid))
      changesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1, offset = 1).map(_.guid) must be(Nil)
    }

    it("authorization") {
      val change = createChange()

      val user = Util.createRandomUser()
      changesDao.findAll(Authorization.User(user.guid), guid = Some(change.guid)).map(_.guid) must be(Nil)
      changesDao.findAll(Authorization.PublicOnly, guid = Some(change.guid)).map(_.guid) must be(Nil)

      val app = applicationsDao.findByGuid(Authorization.All, change.application.guid).get
      Util.createMembership(
        organizationsDao.findByGuid(
          Authorization.All,
          app.organization.guid
        ).get,
        user
      )
      changesDao.findAll(Authorization.User(Util.createdBy.guid), guid = Some(change.guid)).map(_.guid) must be(Seq(change.guid))
    }
  }

}
