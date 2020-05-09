package helpers

import java.util.UUID
import io.apibuilder.api.json.v0.models._

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
  ): Field = {
    Field(
      name = name,
      `type` = `type`,
      required = required,
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
    values: Seq[EnumValue] = Nil,
  ): Enum = {
    Enum(
      values = values,
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
    fields: Seq[Field] = Nil,
    interfaces: Option[Seq[String]] = None,
  ): Model = {
    Model(
      fields = fields,
      interfaces = interfaces,
    )
  }

  def makeOperation(
    method: String = "GET",
    path: String = "/",
  ): Operation = {
    Operation(
      method = method,
      path = path,
    )
  }

  def makeResource(
    operations: Seq[Operation] = Nil,
  ): Resource = {
    Resource(
      operations = operations,
    )
  }

  def makeApiJson(
    name: String = randomString(),
    interfaces: Map[String, Interface] = Map.empty,
    enums: Map[String, Enum] = Map.empty,
    models: Map[String, Model] = Map.empty,
    unions: Map[String, Union] = Map.empty,
    resources: Map[String, Resource] = Map.empty,
  ): ApiJson = {
    ApiJson(
      name = name,
      interfaces = interfaces,
      enums = enums,
      models = models,
      unions = unions,
      resources = resources,
    )
  }
}
