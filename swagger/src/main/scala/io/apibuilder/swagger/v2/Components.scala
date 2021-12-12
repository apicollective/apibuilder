package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.apibuilder.swagger.SchemaType
import io.apibuilder.swagger.v2.ComponentsValidator.ComponentSchema
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.{ComposedSchema, Schema}
import io.swagger.v3.oas.{models => swagger}
import lib.Text

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

case class ReferenceType[T](ref: String, value: T)

object ComponentsValidator extends OpenAPIParseHelpers {
  def validate(api: OpenAPI): ValidatedNec[String, Components] = {
    Option(api.getComponents) match {
      case None => Components(
        models = Nil
      ).validNec
      case Some(c) => {
        validateSchemas(c).map { models =>
          Components(
            models = models,
          )
        }
      }
    }
  }

  case class ComponentSchema[T](name: String, schema: Schema[T])
  private[this] def validateSchemas(components: swagger.Components): ValidatedNec[String, Seq[ReferenceType[Model]]] = {
    val all: List[ComponentSchema[_]] = Option(components.getSchemas).map(_.asScala).getOrElse(Nil).toList.map { case (name, schema) =>
      ComponentSchema(name, schema)
    }
    validateSchemasRecursively(all, Nil)
  }

  @tailrec
  private[this] def validateSchemasRecursively(pending: List[ComponentSchema[_]], completed: List[ValidatedNec[String, ReferenceType[Model]]]): ValidatedNec[String, Seq[ReferenceType[Model]]] = {
    pending match {
      case Nil => completed.traverse(identity)
      case one :: rest => {
        validateSchemasRecursively(rest, completed ++ List(validateSchema(one)))
      }
    }
  }

  private[this] def validateSchema[T](c: ComponentSchema[T]): ValidatedNec[String, ReferenceType[Model]] = {
    (
      validateSchemaDescription(c.schema),
      validateSchemaFields(c.schema),
    ).mapN { case (description, fields) =>
      val name = c.name
      println(s"REFERENCE: #/components/schemas/$name")
      println(s"FIELDS: ${fields}")
      ReferenceType(
        s"#/components/schemas/$name",
        Model(
          name = name,
          plural = Text.pluralize(name),
          description = description,
          fields = fields,
          deprecation = deprecation(c.schema.getDeprecated),
          attributes = Nil, // not supported
          interfaces = Nil  // not supported
        )
      )
    }
  }

  private[this] def validateSchemaDescription[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Option[String]] = {
    trimmedString(schema.getDescription).validNec
  }

  private[this] def validateSchemaFields[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Seq[Field]] = {
    trimmedString(schema.getType) match {
      case Some(t) if t == "object" => validateSchemaFieldsObject(schema)
      case Some(t) => s"API Builder does not yet support components/schema of type '$t".invalidNec
      case None => {
        schema match {
          case c: ComposedSchema => validateComposedSchema(c)
          case other => s"TODO: support ${schema.getClass.getName}".invalidNec
        }
      }
    }
  }

  private[this] def validateComposedSchema[T](schema: swagger.media.Schema[T] with ComposedSchema): ValidatedNec[String, Seq[Field]] = {
    (listOfValues(schema.getAllOf), listOfValues(schema.getOneOf), listOfValues(schema.getAnyOf)) match {
      case (allOf, Nil, Nil) => validateAllOf(allOf)
      case (Nil, oneOf, Nil) => validateOneOf(oneOf)
      case (Nil, Nil, anyOf) => validateAnyOf(anyOf)
      case (Nil, Nil, Nil) => s"component could not be identified. Expected to see allOf, oneOf, or anyOf but found none of those".invalidNec
      case (_, _, _) => s"component specified more than 1 value of allOf, oneOf, or anyOf. Expected to see exactly 1 of these".invalidNec
    }
  }

  private[this] def validateAllOf[T](types: Seq[swagger.media.Schema[T]]): ValidatedNec[String, Field] = {
    types.map {

    }
  }

  private[this] def validateSchemaFieldsObject[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Seq[Field]] = {
    val required = Option(schema.getRequired).map(_.asScala).getOrElse(Nil)
    val properties = Option(schema.getProperties).map(_.asScala).getOrElse(Map.empty[String, swagger.media.Schema[T]])
    properties.keys.toList.sorted.map { name =>
      val props = properties(name)
      validateField(name, props, required.contains(name))
    }.traverse(identity)
  }

  private[this] def validateField(
    name: String,
    props: swagger.media.Schema[_],
    required: Boolean,
  ): ValidatedNec[String, Field] = {
    (
      validateType(props),
      validateDefault(props.getDefault),
    ).mapN { case (typ, default) =>
      Field(
        name = name,
        `type` = typ,
        required = required,
        description = trimmedString(props.getDescription),
        deprecation = deprecation(props.getDeprecated),
        default = default,
        minimum = optionalLong(props.getMinimum).orElse {
          optionalLong(props.getMinItems).orElse {
            optionalLong(props.getMinLength)
          }
        },
        maximum = optionalLong(props.getMaximum).orElse {
          optionalLong(props.getMaxItems).orElse {
            optionalLong(props.getMaxLength)
          }
        },
        example = None, // TODO: Add support
        attributes = Nil, // Not supported
        annotations = Nil, // Not supported
      )
    }
  }

  private[this] def validateType(props: swagger.media.Schema[_]): ValidatedNec[String, String] = {
    SchemaType.validateFromSwagger(props.getType, Option(props.getFormat))
  }

  private[this] def validateDefault(value: Any): ValidatedNec[String, Option[String]] = {
    Option(value).map(_.toString).validNec
  }

}

case class Components(models: Seq[ReferenceType[Model]]) {
  private[this] val modelsByRef: Map[String, Model] = models.map { m => m.ref -> m.value }.toMap

  def findModel(ref: String): Option[Model] = modelsByRef.get(ref)
}
