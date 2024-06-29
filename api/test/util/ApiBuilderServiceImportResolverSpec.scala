package util

import db.Authorization
import io.apibuilder.spec.v0.models.{Import, Service}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ApiBuilderServiceImportResolverSpec extends PlaySpec with GuiceOneAppPerSuite
  with helpers.ServiceHelpers
  with db.Helpers
{

  private def apibuilderServiceImportResolver: ApiBuilderServiceImportResolver = app.injector.instanceOf[ApiBuilderServiceImportResolver]

  private def createServiceVersion(
    name: String = "user",
    version: String,
    imports: Seq[Import] = Nil,
  ): Service = {
    val svc = makeService(
      organization = makeOrganization(key = "test"),
      namespace = "test.com",
      application = makeApplication(key = name),
      version = version,
      name = name,
      imports = imports,
    )
    createVersion(svc)
    svc
  }

  private def resolve(service: Service): Seq[String] = {
    apibuilderServiceImportResolver.resolve(Authorization.All, service).map(toLabel)
  }

  private def toLabel(service: Service): String = {
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
            createServiceVersion(version = "1.0.0")
          ),
        )
      )
    ) must equal(
      Seq("test/user/1.0.0")
    )
  }

  "collapses duplicate imports" in {
    val userService = createServiceVersion(version = "1.0.0")
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(userService),
      )
    )
    resolve(service) must equal(Seq("test/user/1.0.0"))
  }

  "keeps different services" in {
    val userService = createServiceVersion(version = "1.0.0", name = "user")
    val orgService = createServiceVersion(version = "2.0.0", name = "org")
    val service = makeService(
      imports = Seq(
        makeImport(userService),
        makeImport(orgService),
      )
    )
    resolve(service).sorted must equal(
      Seq("test/org/2.0.0", "test/user/1.0.0")
    )
  }

  "selects latest version regardless of order of imports" in {
    def test(svc1: Service, svc2: Service, svc3: Service) = {
      resolve(
        makeService(imports = Seq(
          makeImport(svc1), makeImport(svc2), makeImport(svc3)
        ))
      )
    }

    val s1 = createServiceVersion(version = "1.0.0")
    val s2 = createServiceVersion(version = "1.0.1")
    val s3 = createServiceVersion(version = "1.0.1-dev")

    test(s1, s2, s3) must equal(Seq("test/user/1.0.1"))
    test(s1, s3, s2) must equal(Seq("test/user/1.0.1"))

    test(s2, s1, s3) must equal(Seq("test/user/1.0.1"))
    test(s2, s3, s1) must equal(Seq("test/user/1.0.1"))

    test(s3, s1, s2) must equal(Seq("test/user/1.0.1"))
    test(s3, s2, s1) must equal(Seq("test/user/1.0.1"))
  }

  "resolves deep imports" in {
    val svc4 = createServiceVersion(name = "svc4", version = "4")
    val svc3 = createServiceVersion(name = "svc3", version = "3", imports = Seq(makeImport(svc4)))
    val svc2 = createServiceVersion(name = "svc2", version = "2", imports = Seq(makeImport(svc3)))

    resolve(
      makeService(
        name = "svc1",
        version = "0.9.10",
        imports = Seq(makeImport(svc2))
      )
    ).sorted must equal(
      Seq("test/svc2/2", "test/svc3/3", "test/svc4/4")
    )
  }

}
