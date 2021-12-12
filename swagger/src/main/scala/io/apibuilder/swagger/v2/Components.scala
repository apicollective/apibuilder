package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.apibuilder.swagger.SchemaType
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
    resolveOrder(all, Nil).map { s =>
      validateSchema(s)
    }.traverse(identity)
  }

  @tailrec
  private[this] def resolveOrder(pending: List[ComponentSchema[_]], completed: List[ComponentSchema[_]]): List[ComponentSchema[_]] = {
    val (ready, notReady) = pending.partition { c =>
      allTypesResolved(c, completed)
    }
    if (ready.isEmpty) {
      completed ++ pending
    } else {
      resolveOrder(notReady, completed ++ ready)
    }
  }

  private[this] def allTypesResolved(t: ComponentSchema[_], completed: List[ComponentSchema[_]]): Boolean = {
    true // TODO
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

  private[this] def validateSchemaDescription[T](schema: Schema[T]): ValidatedNec[String, Option[String]] = {
    trimmedString(schema.getDescription).validNec
  }

  private[this] def validateSchemaFields[T](schema: Schema[T]): ValidatedNec[String, Seq[Field]] = {
    trimmedString(schema.getType) match {
      case Some(t) if t == "object" => validateSchemaFieldsObject(schema)
      case Some(t) => s"API Builder does not yet support components/schema of type '$t".invalidNec
      case None => {
        schema match {
          case c: ComposedSchema => validateComposedSchema(c)
          case other => s"TODO: support ${other.getClass.getName}".invalidNec
        }
      }
    }
  }

  private[this] def validateComposedSchema(schema: swagger.media.ComposedSchema): ValidatedNec[String, Seq[Field]] = {
    (listOfValues(schema.getAllOf), listOfValues(schema.getOneOf), listOfValues(schema.getAnyOf)) match {
      case (Nil, Nil, Nil) => s"component could not be identified. Expected to see allOf, oneOf, or anyOf but found none of those".invalidNec
      case (allOf, Nil, Nil) => validateAllOf(allOf)
      case (Nil, oneOf, Nil) => validateOneOf(oneOf)
      case (Nil, Nil, anyOf) => validateAnyOf(anyOf)
      case (_, _, _) => s"component specified more than 1 value of allOf, oneOf, or anyOf. Expected to see exactly 1 of these".invalidNec
    }
  }

  private[this] def validateAllOf(types: List[Schema[_]]): ValidatedNec[String, Seq[Field]] = {
    println(s"validateAllOf types: ${types}")
    "allOf is not yet supported".invalidNec
  }

  private[this] def validateOneOf(types: List[Schema[_]]): ValidatedNec[String, Seq[Field]] = {
    println(s"validateOneOf types: ${types}")
    "oneOf is not yet supported".invalidNec
  }

  private[this] def validateAnyOf(types: List[Schema[_]]): ValidatedNec[String, Seq[Field]] = {
    println(s"validateAnyOf types: ${types}")
    "oneOf is not yet supported".invalidNec
  }


  private[this] def validateSchemaFieldsObject[T](schema: Schema[T]): ValidatedNec[String, Seq[Field]] = {
    val required = Option(schema.getRequired).map(_.asScala).getOrElse(Nil)
    val properties = Option(schema.getProperties).map(_.asScala).getOrElse(Map.empty[String, Schema[T]])
    properties.keys.toList.sorted.map { name =>
      val props = properties(name)
      validateField(name, props, required.contains(name))
    }.traverse(identity)
  }

  private[this] def validateField(
    name: String,
    props: Schema[_],
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

  private[this] def validateType(props: Schema[_]): ValidatedNec[String, String] = {
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
