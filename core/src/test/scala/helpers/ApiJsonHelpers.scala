package helpers

import core.TestHelper
import io.apibuilder.api.json.v0.models._
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.spec.v0.models.Service
import play.api.libs.json.{JsValue, Json}

import java.util.UUID

trait ApiJsonHelpers extends ValidatedTestHelpers {

  def setupValidApiJson(json: String): Service = {
    val service = expectValid {
      TestHelper.serviceValidatorFromApiJson(json)
    }
    service
  }

  def setupValidApiJson(apiJson: ApiJson): Service = {
    setupValidApiJson(Json.prettyPrint(Json.toJson(apiJson)))
  }

  def expectErrorsApiJson(apiJson: ApiJson): Seq[String] = {
    expectInvalid {
      TestHelper.serviceValidator(apiJson)
    }
  }

  def expectValidApiJson(apiJson: ApiJson): Service = {
    expectValid {
      TestHelper.serviceValidator(apiJson)
    }
  }

  // random string that will start with a letter as we expect valid identifiers
  def randomName(): String = {
    "a" + UUID.randomUUID.toString
  }

  def makeTemplates(
    models: Option[Map[String, Model]] = None,
    resources: Option[Map[String, Resource]] = None,
  ): Templates = {
    Templates(
      models = models,
      resources = resources
    )
  }

  def makeInterface(
    fields: Option[Seq[Field]] = None,
  ): Interface = {
    Interface(
      fields = fields,
    )
  }

  def makeDeprecation(
    description: Option[String] = None
  ): Deprecation = {
    Deprecation(
      description = description
    )
  }

  def makeTemplateDeclaration(
    name: String = randomName(),
    cast: Option[Map[String, String]] = None
  ): TemplateDeclaration = {
    TemplateDeclaration(
      name = name,
      cast = cast
    )
  }

  def makeAttribute(name: String = randomName()): Attribute = {
    Attribute(
      name = name,
      value = Json.obj()
    )
  }

  def makeAnnotation(): Annotation = {
    Annotation()
  }

  def makeField(
    name: String = randomName(),
    `type`: String = "string",
    required: Boolean = true,
    default: Option[JsValue] = None,
    description: Option[String] = None,
    example: Option[String] = None,
    minimum: Option[Long] = None,
    maximum: Option[Long] = None,
    attributes: Option[Seq[Attribute]] = None,
    annotations: Option[Seq[String]] = None,
  ): Field = {
    Field(
      name = name,
      `type` = `type`,
      required = required,
      default = default,
      description = description,
      example = example,
      minimum = minimum,
      maximum =  maximum,
      attributes = attributes,
      annotations = annotations
    )
  }

  def makeEnumValue(
    name: String = randomName(),
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
    attributes: Option[Seq[Attribute]] = None,
    templates: Option[Seq[TemplateDeclaration]] = None
  ): Model = {
    Model(
      fields = fields,
      interfaces = interfaces,
      plural = plural,
      attributes = attributes,
      templates = templates
    )
  }

  def makeParameter(
    name: String = randomName(),
    `type`: String = "string",
    location: ParameterLocation = ParameterLocation.Query,
  ): Parameter = {
    Parameter(
      name = name,
      `type` = `type`,
      location = location,
    )
  }

  def makeBody(`type`: String = "string") :Body = {
    Body(
      `type` = `type`
    )
  }

  def makeOperation(
    method: String = "GET",
    path: Option[String] = None,
    body: Option[Body] = None,
    parameters: Option[Seq[Parameter]] = None,
    responses: Option[Map[String, Response]] = None,
  ): Operation = {
    Operation(
      method = method,
      path = path,
      body = body,
      parameters = parameters,
      responses = responses
    )
  }

  def makeResponse(`type`: String): Response = {
    Response(
      `type` = `type`,
      headers = None,
      description = None,
      deprecation = None,
      attributes = None,
    )
  }

  def makeResource(
    path: Option[String] = None,
    operations: Seq[Operation] = Nil,
    templates: Option[Seq[TemplateDeclaration]] = None,
  ): Resource = {
    Resource(
      path = path,
      operations = operations,
      templates = templates,
    )
  }

  def makeImport(uri: String = s"https://${randomName()}test.apibuilder.io"): Import = {
    Import(
      uri = uri,
    )
  }

  def makeUnionType(
    `type`: String,
    default: Boolean = false,
    discriminatorValue: Option[String] = None,
  ): UnionType = {
    UnionType(
      `type` = `type`,
      default = default,
      discriminatorValue = discriminatorValue,
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
    name: String = randomName(),
    namespace: Option[String] = None,
    baseUrl: Option[String] = None,
    imports: Seq[Import] = Nil,
    interfaces: Map[String, Interface] = Map.empty,
    templates: Option[Templates] = None,
    enums: Map[String, Enum] = Map.empty,
    models: Map[String, Model] = Map.empty,
    unions: Map[String, Union] = Map.empty,
    resources: Map[String, Resource] = Map.empty,
    annotations: Map[String, Annotation] = Map.empty,
  ): ApiJson = {
    ApiJson(
      name = name,
      namespace = namespace,
      baseUrl = baseUrl,
      imports = imports,
      interfaces = interfaces,
      templates = templates,
      enums = enums,
      models = models,
      unions = unions,
      resources = resources,
      annotations = annotations
    )
  }

  def toService(apiJson: ApiJson): Service = {
    expectValid {
      TestHelper.serviceValidator(apiJson)
    }
  }

}
