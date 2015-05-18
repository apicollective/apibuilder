package db

import com.gilt.apidoc.internal.v0.models.{DifferenceBreaking, DifferenceNonBreaking}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ChangesDaoSpec extends FunSpec with Matchers {

  describe("with a valid from and to version") {

    lazy val fromVersion = Util.createVersion(version = "1.0.0")
    lazy val toVersion = Util.createVersion(
      version = "1.0.1",
      application = ApplicationsDao.findByGuid(Authorization.All, fromVersion.application.guid).get
    )

    it("upsert is a no-op with no differences") {
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Nil)
    }

    it("upsert can record multiple changes") {
      val diffs = Seq(
        DifferenceBreaking("Breaking difference - " + UUID.randomUUID.toString),
        DifferenceNonBreaking("Non breaking difference - " + UUID.randomUUID.toString)
      )

      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, diffs)
    }

    it("upsert does not throw error on duplicate") {
      val diff = DifferenceBreaking("Breaking difference - " + UUID.randomUUID.toString)
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff, diff))
      ChangesDao.upsert(Util.createdBy, fromVersion, toVersion, Seq(diff))
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
