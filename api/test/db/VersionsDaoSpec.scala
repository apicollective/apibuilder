package db

import com.gilt.apidoc.v0.models.{ApplicationForm, Version, Visibility}
import com.gilt.apidoc.spec.v0.models.{Application, Organization, Service}
import com.gilt.apidoc.spec.v0.models.json._
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import play.api.libs.json.{Json, JsObject}

class VersionsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  val applicationKey = "test-" + UUID.randomUUID.toString

  private lazy val application = {
    val applicationForm = ApplicationForm(
      name = applicationKey,
      key = Some(applicationKey),
      description = None,
      visibility = Visibility.Organization
    )
    ApplicationsDao.create(Util.createdBy, Util.testOrg, applicationForm)
  }

  private val OriginalJson = Json.obj("name" -> UUID.randomUUID.toString)

  private lazy val service = Service(
    name = applicationKey,
    organization = Organization(key = "test"),
    application = Application(key = applicationKey),
    namespace = "test." + key,
    version = "0.0.1-dev",
    headers = Nil,
    imports = Nil,
    enums = Nil,
    models = Nil,
    unions = Nil,
    resources = Nil
  )

  it("create") {
    val version = VersionsDao.create(Util.createdBy, application, "1.0.0", OriginalJson, service)
    version.version should be("1.0.0")
  }

  it("findByApplicationAndVersion") {
    VersionsDao.create(Util.createdBy, application, "1.0.1", OriginalJson, service)
    VersionsDao.findByApplicationAndVersion(Authorization.All, application, "1.0.1").map(_.service) should be(Some(service))
  }

  it("soft delete") {
    val version1 = VersionsDao.create(Util.createdBy, application, "1.0.2", OriginalJson, service)
    VersionsDao.softDelete(Util.createdBy, version1)

    val version2 = VersionsDao.create(Util.createdBy, application, "1.0.2", OriginalJson, service)
    version2.copy(guid = version1.guid) should be(version1)
    version2.guid shouldNot be(version1.guid)
  }

}
