package db

import com.gilt.apidoc.models.{ApplicationForm, Version, Visibility}
import com.gilt.apidocspec.models.Service
import com.gilt.apidocspec.models.json._
import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID
import play.api.libs.json.{Json, JsObject}

class VersionsDaoSpec extends FlatSpec {

  new play.core.StaticApplication(new java.io.File("."))

  val key = UUID.randomUUID.toString

  private lazy val application = {
    val applicationForm = ApplicationForm(
      name = key,
      key = Some(key),
      description = None,
      visibility = Visibility.Organization
    )
    ApplicationsDao.create(Util.createdBy, Util.testOrg, applicationForm)
  }

  private val OriginalJson = Json.obj("name" -> UUID.randomUUID.toString)

  private lazy val service = Service(
    name = key,
    key = key,
    namespace = "test." + key,
    version = "0.0.1-dev",
    headers = Nil,
    imports = Nil,
    enums = Nil,
    models = Nil,
    resources = Nil
  )

  it should "create" in {
    val version = VersionsDao.create(Util.createdBy, application, "1.0.0", OriginalJson, service)
    assertEquals("1.0.0", version.version)
  }

  it should "findByApplicationAndVersion" in {
    VersionsDao.create(Util.createdBy, application, "1.0.1", OriginalJson, service)
    assertEquals(
      Some(Json.toJson(service).as[JsObject]),
      VersionsDao.findByApplicationAndVersion(Authorization.All, application, "1.0.1").map(_.service)
    )
  }

  it should "soft delete" in {
    val version1 = VersionsDao.create(Util.createdBy, application, "1.0.2", OriginalJson, service)
    VersionsDao.softDelete(Util.createdBy, version1)

    val version2 = VersionsDao.create(Util.createdBy, application, "1.0.2", OriginalJson, service)
    assertEquals(version1, version2.copy(guid = version1.guid))
    assertNotEquals(version1.guid, version2.guid)
  }

}
