package db

import io.apibuilder.api.v0.models.{Application, Change, DiffBreaking, DiffNonBreaking, Organization, Version}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class ChangesDaoSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  private[this] def getApplication(version: Version): Application = {
    applicationsDao.findByGuid(Authorization.All, version.application.guid).getOrElse {
      sys.error("Could not find application for version: " + version)
    }
  }

  private[this] def createChange(
    description: String = "Breaking difference - " + UUID.randomUUID.toString,
    org: Organization = createOrganization()
  ): Change = {
    val app = createApplication(org = org)
    val fromVersion = createVersion(application = app, version = "1.0.0")
    val toVersion = createVersion(application = app, version = "1.0.1")
    val diff = DiffBreaking(description)
    changesDao.upsert(testUser, fromVersion, toVersion, Seq(diff))

    changesDao.findAll(
      Authorization.All,
      fromVersionGuid = Some(fromVersion.guid)
    ).head
  }

  "with a valid from and to version" must {

    "upsert is a no-op with no differences" in {
      val fromVersion = createVersion(version = "1.0.0")
      val toVersion = createVersion(version = "1.0.1", application = getApplication(fromVersion))

      changesDao.upsert(testUser, fromVersion, toVersion, Nil)
      changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ) must be(Nil)
    }

    "upsert can record multiple changes" in {
      val fromVersion = createVersion(version = "1.0.0")
      val toVersion = createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val breaking = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      val nonBreaking = DiffNonBreaking("Non breaking difference - " + UUID.randomUUID.toString)

      changesDao.upsert(testUser, fromVersion, toVersion, Seq(breaking, nonBreaking))

      val actual = changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff)

      actual.size must be(2)
      actual.contains(breaking) must be(true)
      actual.contains(nonBreaking) must be(true)
    }

    "upsert does not throw error on duplicate" in {
      val fromVersion = createVersion(version = "1.0.0")
      val toVersion = createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val diff = DiffBreaking("Breaking difference - " + UUID.randomUUID.toString)
      changesDao.upsert(testUser, fromVersion, toVersion, Seq(diff, diff))
      changesDao.upsert(testUser, fromVersion, toVersion, Seq(diff))

      changesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.diff) must be(Seq(diff))
    }

  }

  "upsert raises errors if applications are different" in {
    val fromVersion = createVersion(version = "1.0.0")
    val toVersion = createVersion(version = "1.0.1")

    intercept[AssertionError] {
      changesDao.upsert(testUser, fromVersion, toVersion, Nil)
    }.getMessage must be("assertion failed: Versions must belong to same application")
  }

  "findAll" must {

    "guid" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, guid = Some(change.guid)).map(_.guid) must be(Seq(change.guid))
    }

    "organizationGuid" in {
      val org = createOrganization()
      val change = createChange(org = org)
      changesDao.findAll(Authorization.All, organizationGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, organizationGuid = Some(org.guid)).map(_.guid) must be(Seq(change.guid))
    }

    "organizationKey" in {
      val org = createOrganization()
      val change = createChange(org = org)
      changesDao.findAll(Authorization.All, organizationKey = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, organizationKey = Some(org.key)).map(_.guid) must be(Seq(change.guid))
    }

    "applicationGuid" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, applicationGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, applicationGuid = Some(change.application.guid)).map(_.guid) must be(Seq(change.guid))
    }

    "applicationKey" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, applicationKey = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, applicationKey = Some(change.application.key)).map(_.guid) must be(Seq(change.guid))
    }

    "fromVersionGuid" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, fromVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, fromVersionGuid = Some(change.fromVersion.guid)).map(_.guid) must be(Seq(change.guid))
    }

    "toVersionGuid" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, toVersionGuid = Some(UUID.randomUUID)) must be(Nil)
      changesDao.findAll(Authorization.All, toVersionGuid = Some(change.toVersion.guid)).map(_.guid) must be(Seq(change.guid))
    }

    "fromVersion" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(change.guid), fromVersion = Some(change.fromVersion.version)).map(_.guid) must be(Seq(change.guid))
    }

    "toVersion" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, toVersion = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, guid = Some(change.guid), toVersion = Some(change.toVersion.version)).map(_.guid) must be(Seq(change.guid))
    }

    "description" in {
      val desc = UUID.randomUUID.toString
      val change = createChange(description = desc)
      changesDao.findAll(Authorization.All, description = Some(UUID.randomUUID.toString)) must be(Nil)
      changesDao.findAll(Authorization.All, description = Some(desc)).map(_.guid) must be(Seq(change.guid))
    }

    "limit and offset" in {
      val change = createChange()
      changesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1).map(_.guid) must be(Seq(change.guid))
      changesDao.findAll(Authorization.All, guid = Some(change.guid), limit = 1, offset = 1).map(_.guid) must be(Nil)
    }

    "authorization" in {
      val change = createChange()

      val user = createRandomUser()
      changesDao.findAll(Authorization.User(user.guid), guid = Some(change.guid)).map(_.guid) must be(Nil)
      changesDao.findAll(Authorization.PublicOnly, guid = Some(change.guid)).map(_.guid) must be(Nil)

      val app = applicationsDao.findByGuid(Authorization.All, change.application.guid).get
      createMembership(
        organizationsDao.findByGuid(
          Authorization.All,
          app.organization.guid
        ).get,
        user
      )
      changesDao.findAll(Authorization.User(testUser.guid), guid = Some(change.guid)).map(_.guid) must be(Seq(change.guid))
    }
  }

}
