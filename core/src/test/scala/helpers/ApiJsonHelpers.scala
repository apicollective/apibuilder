package helpers

import java.util.UUID

import core.TestHelper
import io.apibuilder.api.json.v0.models._
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.spec.v0.models.Service
import play.api.libs.json.Json

trait ApiJsonHelpers {

  // random string that will start with a letter as we expect valid identifiers
  private[this] def randomString(): String = {
    "a" + UUID.randomUUID.toString
  }

  def makeInterface(
    fields: Option[Seq[Field]] = None,
  ): Interface = {
    Interface(
      fields = fields,
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

  def makeEnumValue(
    name: String = randomString(),
  ): EnumValue = {
    EnumValue(
      name = name,
    )
  }

  def makeEnum(
    plural: Option[String] = None,
    values: Seq[EnumValue] = Nil,
  ): Enum = {
    Enum(
      plural = plural,
      values = values,
    )
  }

  def makeModelWithField(
    interfaces: Option[Seq[String]] = None,
  ): Model = {
    makeModel(
      interfaces = interfaces,
      fields = Seq(
        makeField(name = "id"),
      )
    )
  }

  def makeModel(
    fields: Seq[Field] = Nil,
    interfaces: Option[Seq[String]] = None,
    plural: Option[String] = None,
  ): Model = {
    Model(
      fields = fields,
      interfaces = interfaces,
      plural = plural,
    )
  }

  def makeParameter(
    name: String = randomString(),
    `type`: String = "string",
    location: ParameterLocation = ParameterLocation.Query,
  ): Parameter = {
    Parameter(
      name = name,
      `type` = `type`,
      location = location,
    )
  }

  def makeOperation(
    method: String = "GET",
    path: String = "/",
    parameters: Option[Seq[Parameter]] = None,
  ): Operation = {
    Operation(
      method = method,
      path = path,
      parameters = parameters,
    )
  }

  def makeResource(
    operations: Seq[Operation] = Nil,
  ): Resource = {
    Resource(
      operations = operations,
    )
  }

  def makeImport(uri: String = s"https://${randomString()}test.apibuilder.io"): Import = {
    Import(
      uri = uri,
    )
  }

  def makeUnionType(
    `type`: String,
    default: Boolean = false,
  ): UnionType = {
    UnionType(
      `type` = `type`,
      default = default,
    )
  }


  def makeUnion(
    discriminator: Option[String] = None,
    plural: Option[String] = None,
    types: Seq[UnionType] = Nil,
    interfaces: Option[Seq[String]] = None,
  ): Union = {
    Union(
      discriminator = discriminator,
      plural = plural,
      types = types,
      interfaces = interfaces,
    )
  }

  def makeApiJson(
    name: String = randomString(),
    namespace: Option[String] = None,
    baseUrl: Option[String] = None,
    imports: Seq[Import] = Nil,
    interfaces: Map[String, Interface] = Map.empty,
    enums: Map[String, Enum] = Map.empty,
    models: Map[String, Model] = Map.empty,
    unions: Map[String, Union] = Map.empty,
    resources: Map[String, Resource] = Map.empty,
  ): ApiJson = {
    ApiJson(
      name = name,
      namespace = namespace,
      baseUrl = baseUrl,
      imports = imports,
      interfaces = interfaces,
      enums = enums,
      models = models,
      unions = unions,
      resources = resources,
    )
  }

  def toService(apiJson: ApiJson): Service = {
    val validator = TestHelper.serviceValidator(apiJson)
    if (validator.errors().nonEmpty) {
      println(Json.prettyPrint(Json.toJson(apiJson)))
      sys.error(s"Cannot convert to API Builder Spec Service Model:\n  - ${validator.errors().mkString("\n  - ")}")
    }
    validator.service()
  }

}
