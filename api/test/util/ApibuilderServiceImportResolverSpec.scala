package util

import db.Authorization
import io.apibuilder.api.v0.models.{Original, Version}
import io.apibuilder.spec.v0.models.Service
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApibuilderServiceImportResolverSpec extends PlaySpec with GuiceOneAppPerSuite
  with helpers.ServiceHelpers
  with db.Helpers
{

  private[this] def apibuilderServiceImportResolver: ApibuilderServiceImportResolver = app.injector.instanceOf[ApibuilderServiceImportResolver]

  def createVersion(service: Service): Version = {
    val org = organizationsDao.findByKey(Authorization.All, service.organization.key).getOrElse {
      createOrganization(key = Some(service.organization.key))
    }
    val application = applicationsDao.findByOrganizationKeyAndApplicationKey(Authorization.All, org.key, service.application.key).getOrElse {
      createApplicationByKey(
        org = org,
        key = service.application.key,
      )
    }
    val original = createOriginal(service)
    versionsDao.create(testUser, application, service.version, original, service)
  }

  def toLabel(service: Service): String = {
    s"${service.organization.key}/${service.application.key}/${service.version}"
  }

  "resolve service with no imports" in {
    val service = makeService()
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(Nil)
  }

  "resolve service with 1 import" in {
    val userService = makeService(
      organization = makeOrganization(key = "test"),
      application = makeApplication(key = "user"),
      version = "1.0.0",
      name = "user",
    )
    val v = createVersion(userService)
    println(s"version created: ${v}")
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(userService),
      )
    )
    apibuilderServiceImportResolver.resolve(Authorization.All, service).map(toLabel) must equal(
      Seq("test/user/1.0.0")
    )
  }

  "callapses duplicate imports" in {
    val userService = makeService(name = "user")
    //createVersion(userService)
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(userService),
      )
    )
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(
      Seq("1")
    )
  }

  "collapses duplicate imports" in {
    val service = makeService()
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(Nil)
  }

}
