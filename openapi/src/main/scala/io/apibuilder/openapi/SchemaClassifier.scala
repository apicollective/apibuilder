package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.Deprecation
import io.apibuilder.validation.ScalarType
import sttp.apispec.{AnySchema, ExampleSingleValue, Schema, SchemaLike, SchemaType}

import scala.collection.immutable.ListMap

object SchemaClassifier {

  def classify(schemas: ListMap[String, SchemaLike]): SchemaClassification = {
    val classified = schemas.toSeq.map {
      case (name, s: Schema) =>
        val kind = classifySchema(s)
        val fields = kind match {
          case SchemaKind.Object => classifyFields(name, s)
          case _ => Seq.empty
        }
        ClassifiedSchema(name, kind, Some(s), fields)
      case (name, _) =>
        ClassifiedSchema(name, SchemaKind.Skip, None, Seq.empty)
    }
    SchemaClassification(classified)
  }

  private[openapi] def classifySchema(s: Schema): SchemaKind = {
    val members = unionMembers(s)
    if (SchemaResolver.detectMapType(s).isDefined) SchemaKind.Alias
    else if (s.properties.nonEmpty || hasType(s, SchemaType.Object)) SchemaKind.Object
    else if (hasType(s, SchemaType.String) && s.`enum`.isDefined) SchemaKind.StringEnum
    else if (hasType(s, SchemaType.Array)) SchemaKind.Array
    else if (members.nonEmpty && s.properties.isEmpty && collectRefs(members).nonEmpty) SchemaKind.Union
    else if (
      s.$ref.isDefined ||
      (s.allOf.nonEmpty && s.properties.isEmpty) ||
      (hasType(s, SchemaType.String) && s.`enum`.isEmpty)
    ) SchemaKind.Alias
    else SchemaKind.Skip
  }

  private[openapi] def classifyField(s: Schema): Option[FieldKind] = {
    import SchemaResolver.refName

    lazy val fromRef: Option[FieldKind] =
      s.$ref.map(ref => FieldKind.Ref(refName(ref)))

    lazy val fromAllOf: Option[FieldKind] = Option
      .when(s.allOf.nonEmpty) {
        s.allOf
          .collectFirst { case r: Schema if r.$ref.isDefined => refName(r.$ref.get) }
          .map(FieldKind.AllOfRef(_))
      }
      .flatten

    lazy val fromNumber: Option[FieldKind] =
      Option.when(hasType(s, SchemaType.Number))(FieldKind.Number)

    lazy val fromOneOfAnyOf: Option[FieldKind] = {
      val members = unionMembers(s)
      Option
        .when(members.nonEmpty && s.properties.isEmpty) {
          collectRefs(members).headOption.map(FieldKind.Ref(_))
        }
        .flatten
    }

    lazy val fromArrayRef: Option[FieldKind] =
      Option
        .when(hasType(s, SchemaType.Array)) {
          s.items.collectFirst {
            case items: Schema if items.$ref.isDefined => Some(FieldKind.ArrayRef(refName(items.$ref.get)))
            case items: Schema if unionMembers(items).nonEmpty =>
              collectRefs(unionMembers(items)).headOption.map(FieldKind.ArrayRef(_))
          }.flatten
        }
        .flatten

    lazy val fromArrayEnum: Option[FieldKind] =
      Option
        .when(hasType(s, SchemaType.Array)) {
          s.items.collectFirst {
            case items: Schema if hasType(items, SchemaType.String) && items.`enum`.isDefined =>
              FieldKind.ArrayEnum(items)
          }
        }
        .flatten

    lazy val fromEnum: Option[FieldKind] =
      Option.when(hasType(s, SchemaType.String) && s.`enum`.isDefined)(FieldKind.InlineEnum(s))

    lazy val fromMap: Option[FieldKind] =
      s.additionalProperties.map(ap => FieldKind.MapType(SchemaResolver.mapValueType(ap)))

    lazy val fromArraySimple: Option[FieldKind] =
      Option
        .when(hasType(s, SchemaType.Array)) {
          s.items.collectFirst {
            case items: Schema if items.`enum`.isEmpty =>
              SchemaConverter.simpleType(items).map(FieldKind.ArraySimple.apply)
          }.flatten
        }
        .flatten

    lazy val fromSimple: Option[FieldKind] =
      Option.when(s.`enum`.isEmpty)(SchemaConverter.simpleType(s).map(FieldKind.Primitive.apply)).flatten

    lazy val fromDefault: Option[FieldKind] =
      Option.when(s.description.isDefined || s.properties.nonEmpty)(FieldKind.DefaultedString)

    fromRef orElse fromAllOf orElse fromOneOfAnyOf orElse fromNumber orElse fromArrayRef orElse
      fromArrayEnum orElse fromEnum orElse fromMap orElse fromArraySimple orElse fromSimple orElse fromDefault
  }

  case class FieldAnnotations(
    default: Option[String],
    deprecation: Option[Deprecation],
    example: Option[String],
    ignoredFormat: Option[String],
  )

  object FieldAnnotations {
    val empty: FieldAnnotations = FieldAnnotations(None, None, None, None)

    def fromSchema(s: Schema): FieldAnnotations = FieldAnnotations(
      default = s.default.collect { case ExampleSingleValue(v) => v.toString },
      deprecation = s.deprecated.collect { case true => Deprecation() },
      example = s.examples
        .flatMap(_.collectFirst { case ExampleSingleValue(v) => v.toString }),
      ignoredFormat = SchemaConverter.ignoredFormat(s),
    )
  }

  def extractBounds(s: Schema): (Option[Long], Option[Long]) = {
    val min = s.minimum
      .map(_.toLong)
      .orElse(s.minLength.map(_.toLong))
      .orElse(s.minItems.map(_.toLong))
    val max = s.maximum
      .map(_.toLong)
      .orElse(s.maxLength.map(_.toLong))
      .orElse(s.maxItems.map(_.toLong))
    (min, max)
  }

  private[openapi] def collectRefs(schemas: List[SchemaLike]): Seq[String] =
    schemas.collect { case s: Schema if s.$ref.isDefined => SchemaResolver.refName(s.$ref.get) }

  private[openapi] def unionMembers(s: Schema): List[SchemaLike] =
    s.oneOf ++ s.anyOf

  private def hasType(s: Schema, st: SchemaType): Boolean =
    SchemaResolver.hasType(s, st)

  private def classifyFields(schemaName: String, schema: Schema): Seq[ClassifiedField] = {
    val required = schema.required
    schema.properties.toSeq.map { case (fieldName, sl) =>
      classifyFieldEntry(schemaName, fieldName, sl, required)
    }
  }

  private def classifyFieldEntry(
    schemaName: String,
    fieldName: String,
    sl: SchemaLike,
    required: List[String],
  ): ClassifiedField = sl match {
    case s: Schema =>
      val kind = classifyField(s)
      val (min, max) = extractBounds(s)
      val ann = FieldAnnotations.fromSchema(s)
      ClassifiedField(
        schemaName = schemaName,
        fieldName = fieldName,
        kind = kind,
        description = s.description,
        required = required.contains(fieldName),
        minimum = min,
        maximum = max,
        annotations = ann,
      )
    case AnySchema.Anything =>
      ClassifiedField(
        schemaName = schemaName,
        fieldName = fieldName,
        kind = Some(FieldKind.Primitive(ScalarType.JsonType)),
        description = None,
        required = required.contains(fieldName),
        minimum = None,
        maximum = None,
        annotations = FieldAnnotations.empty,
      )
    case AnySchema.Nothing =>
      ClassifiedField(
        schemaName = schemaName,
        fieldName = fieldName,
        kind = None,
        description = None,
        required = required.contains(fieldName),
        minimum = None,
        maximum = None,
        annotations = FieldAnnotations.empty,
      )
  }
}
