package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.swagger.v3.oas.models.{OpenAPI, info}
import io.swagger.v3.oas.{models => swagger}
import lib.Text

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

  private[this] def validateSchemas(components: swagger.Components): ValidatedNec[String, Seq[ReferenceType[Model]]] = {
    Option(components.getSchemas).map(_.asScala).getOrElse(Nil).map { case (ref, schema) =>
      validateSchema(ref, schema)
    }.toList.traverse(identity)
  }

  private[this] def validateSchema[T](ref: String, schema: swagger.media.Schema[T]): ValidatedNec[String, ReferenceType[Model]] = {
    (
      validateSchemaName(schema),
      validateSchemaDescription(schema),
      validateSchemaFields(schema),
      validateSchemaDeprecation(schema)
      ).mapN { case (name, description, fields, deprecation) =>
      ReferenceType(
        ref,
        Model(
          name = name,
          plural = Text.pluralize(name),
          description = description,
          fields = fields,
          deprecation = deprecation,
          attributes = Nil, // not supported
          interfaces = Nil  // not supported
        )
      )
    }
  }

  private[this] def validateSchemaName[T](schema: swagger.media.Schema[T]): ValidatedNec[String, String] = {
    trimmedString(schema.getName) match {
      case None => s"components/schema name must be non blank".invalidNec
      case Some(n) => n.validNec
    }
  }

  private[this] def validateSchemaDescription[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Option[String]] = {
    trimmedString(schema.getDescription).validNec
  }

  private[this] def validateSchemaFields[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Seq[Field]] = {
    println(s"DISCRIMINATOR: ${schema.getDiscriminator}")
    "TODO".invalidNec
  }

  private[this] def validateSchemaDeprecation[T](schema: swagger.media.Schema[T]): ValidatedNec[String, Option[Deprecation]] = {
    Option(schema.getDeprecated) match {
      case Some(v) if v => Some(Deprecation()).validNec
      case _ => None.validNec
    }
  }
}

case class Components(models: Seq[ReferenceType[Model]]) {
  private[this] val modelsByRef: Map[String, Model] = models.map { m => m.ref -> m.value }.toMap

  def findModel(ref: String): Option[Model] = modelsByRef.get(ref)
}
