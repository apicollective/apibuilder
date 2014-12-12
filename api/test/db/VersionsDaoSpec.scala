package db

import com.gilt.apidoc.models.{Version, Visibility}
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class VersionsDaoSpec extends FlatSpec {

  new play.core.StaticApplication(new java.io.File("."))

  private lazy val service = {
    val serviceForm = ServiceForm(
      name = UUID.randomUUID.toString,
      description = None,
      visibility = Visibility.Organization
    )
    ServicesDao.create(Util.createdBy, Util.testOrg, serviceForm)
  }

  it should "create" in {
    val version = VersionsDao.create(Util.createdBy, service, "1.0.0", "{}")
    assertEquals("1.0.0", version.version)
  }

  it should "findByServiceAndVersion" in {
    val version = VersionsDao.create(Util.createdBy, service, "1.0.1", "{}")
    assertEquals(Version(version.guid, version.version, "{}"),
                 VersionsDao.findByServiceAndVersion(service, version.version).get)
  }

  it should "soft delete" in {
    val version1 = VersionsDao.create(Util.createdBy, service, "1.0.2", "{}")
    VersionsDao.softDelete(Util.createdBy, Version(guid = version1.guid, version = version1.version, json = "{}"))

    val version2 = VersionsDao.create(Util.createdBy, service, "1.0.2", "{}")
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
