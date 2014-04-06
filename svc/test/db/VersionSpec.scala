package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class VersionSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  private lazy val service = {
    val name = UUID.randomUUID.toString
    Service.findByOrganizationAndName(Util.testOrg, name).getOrElse {
      Service.create(Util.createdBy, Util.testOrg, name)
    }
  }

  it should "create" in {
    val version = Version.create(Util.createdBy, service, "1.0.0", "{}")
    assertEquals("1.0.0", version.version)
  }

  it should "findByServiceAndVersion" in {
    val version = Version.create(Util.createdBy, service, "1.0.1", "{}")
    assertEquals(version, Version.findByServiceAndVersion(service, version.version).get)
  }

  it should "soft delete" in {
    val version1 = Version.create(Util.createdBy, service, "1.0.2", "{}")
    version1.softDelete(Util.createdBy)

    val version2 = Version.create(Util.createdBy, service, "1.0.2", "{}")
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
