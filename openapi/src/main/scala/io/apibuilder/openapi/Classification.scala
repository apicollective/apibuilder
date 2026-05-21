package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.Header
import io.apibuilder.validation.ScalarType
import sttp.apispec.Schema
import sttp.apispec.openapi.OpenAPI

import scala.collection.immutable.ListMap

sealed trait SchemaKind
object SchemaKind {
  case object Object extends SchemaKind
  case object StringEnum extends SchemaKind
  case object Array extends SchemaKind
  case object Union extends SchemaKind
  case object Alias extends SchemaKind
  case object Skip extends SchemaKind
}

sealed trait FieldKind
object FieldKind {
  case class Ref(typeName: String) extends FieldKind
  case class AllOfRef(typeName: String) extends FieldKind
  case object Number extends FieldKind
  case class ArrayRef(typeName: String) extends FieldKind
  case class ArrayEnum(itemsSchema: Schema) extends FieldKind
  case class InlineEnum(enumSchema: Schema) extends FieldKind
  case class ArraySimple(scalarType: ScalarType) extends FieldKind
  case class MapType(valueType: String) extends FieldKind
  case class Primitive(scalarType: ScalarType) extends FieldKind
  case object DefaultedString extends FieldKind
}

case class ClassifiedField(
  schemaName: String,
  fieldName: String,
  kind: Option[FieldKind],
  description: Option[String],
  required: Boolean,
  minimum: Option[Long],
  maximum: Option[Long],
  annotations: SchemaClassifier.FieldAnnotations,
)

case class ClassifiedSchema(
  name: String,
  kind: SchemaKind,
  schema: Option[Schema],
  fields: Seq[ClassifiedField],
)

case class SchemaClassification(
  schemas: Seq[ClassifiedSchema],
)

case class Classification(
  classification: SchemaClassification,
  modelReferences: Map[String, String],
  pathResult: PathConversionResult,
  securityHeaders: Seq[Header],
  unsupportedFeatures: Seq[String],
  openApi: OpenAPI,
)

object Classification {

  def fromOpenApi(openApi: OpenAPI, namingConfig: NamingConfig, filterHeaders: Set[String]): Classification = {
    val schemas = openApi.components
      .map(_.schemas)
      .getOrElse(ListMap.empty)

    val modelReferences = SchemaResolver.buildModelReferences(schemas)
    val classification = SchemaClassifier.classify(schemas)

    val requestBodies = openApi.components
      .map(_.requestBodies)
      .getOrElse(ListMap.empty)

    val securitySchemes = openApi.components
      .map(_.securitySchemes)
      .getOrElse(ListMap.empty)

    val convertibleSchemeNames = securitySchemes.collect {
      case (name, Right(scheme)) if SecurityConverter.isConvertible(scheme) => name
    }.toSet

    val pathConverter = new PathConverter(modelReferences, namingConfig, filterHeaders, requestBodies, convertibleSchemeNames)
    val pathResult = pathConverter.convertPaths(openApi.paths)

    val securityResult = SecurityConverter.convertSecuritySchemes(securitySchemes)
    val unsupportedFeatures = ConversionReport.detectUnsupportedFeatures(openApi) ++ securityResult.degradedNotes

    Classification(classification, modelReferences, pathResult, securityResult.headers, unsupportedFeatures, openApi)
  }
}
