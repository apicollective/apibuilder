package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class VersionSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val service = {
    val name = UUID.randomUUID.toString
    ServiceDao.findByOrganizationAndName(Util.testOrg, name).getOrElse {
      ServiceDao.create(Util.createdBy, Util.testOrg, name)
    }
  }

  it should "create" in {
    val version = VersionDao.create(Util.createdBy, service, "1.0.0", "{}")
    assertEquals("1.0.0", version.version)
  }

  it should "findByServiceAndVersion" in {
    val version = VersionDao.create(Util.createdBy, service, "1.0.1", "{}")
    assertEquals(Version(version.guid, version.version),
                 VersionDao.findByServiceAndVersion(service, version.version).get)
  }

  it should "soft delete" in {
    val version1 = VersionDao.create(Util.createdBy, service, "1.0.2", "{}")
    VersionDao.softDelete(Util.createdBy, Version(guid = version1.guid, version = version1.version))

    val version2 = VersionDao.create(Util.createdBy, service, "1.0.2", "{}")
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
