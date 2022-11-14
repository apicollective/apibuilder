package helpers

import io.apibuilder.spec.v0.models._
import play.api.libs.json.{JsObject, Json}

trait ServiceHelpers extends RandomHelpers {

  def makeOrganization(key: String = randomString()): Organization = {
    Organization(key = key)
  }

  def makeApplication(key: String = randomString()): Application = {
    Application(key = key)
  }

  def makeInfo(): Info = {
    Info(license = None, contact = None)
  }

  def makeEnum(
    name: String = randomString(),
    plural: String = randomString(),
    values: Seq[EnumValue],
  ): Enum = {
    Enum(
      name = name,
      plural = plural,
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
    service: Service = makeService(),
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

  def makeInterface(
    name: String = randomString(),
    plural: String = randomString(),
    fields: Seq[Field] = Nil,
    attributes: Seq[Attribute] = Nil,
  ): Interface = {
    Interface(
      name = name,
      plural = plural,
      fields = fields,
      attributes = attributes,
    )
  }

  def makeAttribute(
    name: String = randomString(),
    value: JsObject = Json.obj(randomString() -> randomString()),
  ): Attribute = {
    Attribute(
      name = name,
      value = value,
      description = None,
      deprecation = None,
    )
  }

  def makeField(
    name: String = randomString(),
    `type`: String = "string",
    required: Boolean = true,
    default: Option[String] = None,
  ): Field = {
    Field(
      name = name,
      `type` = `type`,
      required = required,
      default = default,
    )
  }

  def makeModelWithField(): Model = {
    makeModel(
      fields = Seq(
        makeField(name = "id"),
      )
    )
  }

  def makeModel(
    name: String = randomString(),
    plural: String = randomString(),
    interfaces: Seq[String] = Nil,
    fields: Seq[Field] = Nil,
  ): Model = {
    Model(
      name = name,
      plural = plural,
      interfaces = interfaces,
      fields = fields,
    )
  }

  def makeUnionType(
    `type`: String,
  ): UnionType = {
    UnionType(
      `type` = `type`,
    )
  }


  def makeUnion(
    name: String = randomString(),
    plural: String = randomString(),
    types: Seq[UnionType] = Nil,
  ): Union = {
    Union(
      name = name,
      plural = plural,
      types = types,
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
    interfaces: Seq[Interface] = Nil,
    models: Seq[Model] = Nil,
    unions: Seq[Union] = Nil,
  ): Service = {
    Service(
      name = name,
      organization = organization,
      application = application,
      namespace = namespace,
      version = version,
      info = makeInfo(),
      imports = imports,
      enums = enums,
      interfaces = interfaces,
      models = models,
      unions = unions,
    )
  }
}
