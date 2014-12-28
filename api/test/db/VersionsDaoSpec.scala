package db

import com.gilt.apidoc.models.{ApplicationForm, Version, Visibility}
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID
import play.api.libs.json.{Json, JsObject}

class VersionsDaoSpec extends FlatSpec {

  new play.core.StaticApplication(new java.io.File("."))

  private val EmptyJsObject = Json.parse("{}").as[JsObject]

  private lazy val application = {
    val applicationForm = ApplicationForm(
      name = UUID.randomUUID.toString,
      description = None,
      visibility = Visibility.Organization
    )
    ApplicationsDao.create(Util.createdBy, Util.testOrg, applicationForm)
  }

  it should "create" in {
    val version = VersionsDao.create(Util.createdBy, application, "1.0.0", EmptyJsObject)
    assertEquals("1.0.0", version.version)
  }

  it should "findByApplicationAndVersion" in {
    val version = VersionsDao.create(Util.createdBy, application, "1.0.1", EmptyJsObject)
    assertEquals(Version(version.guid, version.version, EmptyJsObject),
                 VersionsDao.findByApplicationAndVersion(Authorization.All, application, version.version).get)
  }

  it should "soft delete" in {
    val version1 = VersionsDao.create(Util.createdBy, application, "1.0.2", EmptyJsObject)
    VersionsDao.softDelete(Util.createdBy, Version(guid = version1.guid, version = version1.version, json = EmptyJsObject))

    val version2 = VersionsDao.create(Util.createdBy, application, "1.0.2", EmptyJsObject)
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
