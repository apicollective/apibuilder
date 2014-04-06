package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class VersionDaoSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val serviceDao = ServiceDao.upsert(Util.createdBy, Util.testOrg, "test service")

  it should "upsert" in {
    val version = VersionDao.upsert(serviceDao, "1.0.0", "{}")
  }

  it should "find by guid" in {
    val version = VersionDao.upsert(serviceDao, "1.0.0", "{}")
    assertEquals(version, VersionDao.findByGuid(version.guid).get)
  }

  it should "soft delete" in {
    val version1 = VersionDao.upsert(serviceDao, "1.0.0", "{}")
    VersionDao.softDelete(version1)

    val version2 = VersionDao.upsert(serviceDao, "1.0.0", "{}")
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
