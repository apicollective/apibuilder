package util

import db.Authorization
import io.apibuilder.api.v0.models.Version
import io.apibuilder.spec.v0.models.{Import, Service}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApibuilderServiceImportResolverSpec extends PlaySpec with GuiceOneAppPerSuite
  with helpers.ServiceHelpers
  with db.Helpers
{

  private[this] def apibuilderServiceImportResolver: ApibuilderServiceImportResolver = app.injector.instanceOf[ApibuilderServiceImportResolver]

  private[this] def createUserServiceVersion(version: String): Service = {
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
    resolve(makeService(imports = Nil)) must equal(Nil)
  }

  "resolve service with 1 import" in {
    resolve(
      makeService(
        imports = Seq(
          makeImport(
            createUserServiceVersion(version = "1.0.0")
          ),
        )
      )
    ) must equal(
      Seq("test/user/1.0.0")
    )
  }

  "collapses duplicate imports" in {
    val userService = createUserServiceVersion(version = "1.0.0")
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(userService),
      )
    )
    resolve(service) must equal(Seq("test/user/1.0.0"))
  }

  "selects latest version regardless of order of imports" in {
    def test(svc1: Service, svc2: Service) = {
      resolve(
        makeService(imports = Seq(
          makeImport(svc1), makeImport(svc2)
        ))
      )
    }

    val userService1 = createUserServiceVersion(version = "1.0.0")
    val userService2 = createUserServiceVersion(version = "1.0.1")

    test(userService1, userService2) must equal(Seq("test/user/1.0.1"))
    test(userService2, userService1) must equal(Seq("test/user/1.0.1"))
  }

}
