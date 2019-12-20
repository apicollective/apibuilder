package helpers

import java.util.UUID

import io.apibuilder.spec.v0.models._

trait ServiceHelpers {

  def randomString(): String = {
    UUID.randomUUID.toString
  }

  def makeApidoc(): Apidoc = {
    Apidoc(
      version = "1.0",
    )
  }
  def makeOrganization(): Organization = {
    Organization(key = randomString())
  }

  def makeApplication(): Application = {
    Application(key = randomString())
  }

  def makeInfo(): Info = {
    Info(license = None, contact = None)
  }

  def makeEnum(
    name: String = randomString(),
    values: Seq[EnumValue],
  ): Enum = {
    Enum(
      name = name,
      plural = randomString(),
      values = values,
    )
  }

  def makeEnumValue(
    name: String = randomString(),
  ): EnumValue = {
    EnumValue(
      name = name,
    )
  }

  def makeImportUri(service: Service = makeService()): String = {
    s"https://test.apibuilder.io/${service.organization.key}/${service.application.key}/${service.version}/service.json"
  }

  def makeImport(
    service: Service = makeService()
  ): Import = {
    Import(
      uri = makeImportUri(service),
      namespace = service.namespace,
      organization = service.organization,
      application = service.application,
      version = service.version,
      enums = service.enums.map(_.name),
      unions = service.unions.map(_.name),
      models = service.models.map(_.name),
      annotations = service.annotations
    )
  }

  def makeService(
    name: String = randomString(),
    organization: Organization = makeOrganization(),
    application: Application = makeApplication(),
    namespace: String = randomString(),
    version: String = randomString(),
    imports: Seq[Import] = Nil,
    enums: Seq[Enum] = Nil,
  ): Service = {
    Service(
      apidoc = makeApidoc(),
      name = name,
      organization = organization,
      application = application,
      namespace = namespace,
      version = version,
      info = makeInfo(),
      imports = imports,
      enums = enums,
    )
  }
}
