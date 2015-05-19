package db

import com.gilt.apidoc.api.v0.models.{Application, Version}
import com.gilt.apidoc.internal.v0.models.{Change, DifferenceBreaking, DifferenceNonBreaking}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ChangesDaoSpec extends FunSpec with Matchers {

  private def getApplication(version: Version): Application = {
    ApplicationsDao.findByGuid(Authorization.All, version.application.guid).getOrElse {
      sys.error("Could not find application for version: " + version)
    }
  }

  private def createChange(): Change = {
    val fromVersion = Util.createVersion(version = "1.0.0")
    val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))
    val diff = DifferenceBreaking("Breaking difference - " + UUID.randomUUID.toString)
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

      val diffs = Seq(
        DifferenceBreaking("Breaking difference - " + UUID.randomUUID.toString),
        DifferenceNonBreaking("Non breaking difference - " + UUID.randomUUID.toString)
      )

      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, diffs)

      ChangesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.difference) should be(diffs)
    }

    it("upsert does not throw error on duplicate") {
      val fromVersion = Util.createVersion(version = "1.0.0")
      val toVersion = Util.createVersion(version = "1.0.1", application = getApplication(fromVersion))

      val diff = DifferenceBreaking("Breaking difference - " + UUID.randomUUID.toString)
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff, diff))
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))

      ChangesDao.findAll(
        Authorization.All,
        fromVersionGuid = Some(fromVersion.guid)
      ).map(_.difference) should be(Seq(diff))
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

    it("applicationGuid") {
      val change = createChange()
      ChangesDao.findAll(Authorization.All, applicationGuid = Some(UUID.randomUUID)) should be(Nil)
      ChangesDao.findAll(Authorization.All, applicationGuid = Some(change.application.guid)).map(_.guid) should be(Seq(change.guid))
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

  }

}
