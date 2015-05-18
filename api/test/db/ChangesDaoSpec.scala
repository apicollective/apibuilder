package db

import com.gilt.apidoc.api.v0.models.Version
import com.gilt.apidoc.internal.v0.models.{DifferenceBreaking, DifferenceNonBreaking}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ChangesDaoSpec extends FunSpec with Matchers {

  describe("with a valid from and to version") {

    def getApplication(version: Version) = ApplicationsDao.findByGuid(Authorization.All, version.application.guid).get

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

}
