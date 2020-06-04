package util

import db.Authorization
import io.apibuilder.api.v0.models.Version
import io.apibuilder.spec.v0.models.Service
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApibuilderServiceImportResolverSpec extends PlaySpec with GuiceOneAppPerSuite
  with helpers.ServiceHelpers
  with db.Helpers
{

  private[this] def apibuilderServiceImportResolver: ApibuilderServiceImportResolver = app.injector.instanceOf[ApibuilderServiceImportResolver]

  private[this] def makeUserService(version: String): Service = {
    val svc = makeService(
      organization = makeOrganization(key = "test"),
      application = makeApplication(key = "user"),
      version = version,
      name = "user",
    )
    createVersion(svc)
    svc
  }

  private[this] def resolve(service: Service): Seq[String] = {
    apibuilderServiceImportResolver.resolve(Authorization.All, service).map(toLabel)
  }

  private[this] def toLabel(service: Service): String = {
    s"${service.organization.key}/${service.application.key}/${service.version}"
  }

  "resolve service with no imports" in {
    val service = makeService()
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(Nil)
  }

  "resolve service with 1 import" in {
    val userService = makeUserService(version = "1.0.0")

    resolve(
      makeService(
        imports = Seq(
          makeImport(userService),
        )
      )
    ) must equal(
      Seq("test/user/1.0.0")
    )
  }

  "collapses duplicate imports" in {
    val userService = makeUserService(version = "1.0.0")
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(userService),
      )
    )
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(
      Seq("test/user/1.0.0")
    )
  }

  "selects latest version" in {
    val userService1 = makeUserService(version = "1.0.0")
    val userService2 = makeUserService(version = "1.0.1")
    val service = makeService(
      imports = Seq(
        makeImport(userService1),
        makeImport(userService2),
      )
    )
    apibuilderServiceImportResolver.resolve(Authorization.All, service) must equal(
      Seq("test/user/1.0.1")
    )
  }

}
