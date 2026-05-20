package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.{Attribute, Enum, EnumValue, Field, Model, Union, UnionType}
import io.apibuilder.validation.ScalarType
import play.api.libs.json.Json
import sttp.apispec.{ExampleSingleValue, Schema, SchemaType}

import scala.collection.immutable.ListMap

class SchemaConverter(
  modelReferences: Map[String, String],
  config: NamingConfig,
) {

  import NamingUtils._
  import SchemaClassifier.{collectRefs, unionMembers}
  import SchemaConverter._
  import SchemaResolver._

  def convert(classification: SchemaClassification): SchemaConversionResult = {
    val models = Seq.newBuilder[Model]
    val enums = Seq.newBuilder[Enum]
    val unions = Seq.newBuilder[Union]

    val skippedNames = classification.schemas.collect { case cs if cs.kind == SchemaKind.Skip => cs.name }.toSet

    classification.schemas.foreach { cs =>
      cs.kind match {
        case SchemaKind.Object =>
          cs.schema.foreach { s =>
            val (model, objectEnums) = convertObjectSchema(cs.name, s, cs.fields)
            model.foreach(models += _)
            enums ++= objectEnums
          }
        case SchemaKind.StringEnum =>
          cs.schema.foreach(s => enums += convertTopLevelEnum(cs.name, s))
        case SchemaKind.Array =>
          cs.schema.foreach(s => models += convertArraySchema(cs.name, s))
        case SchemaKind.Union =>
          cs.schema.foreach(s => unions += convertUnionSchema(cs.name, s, skippedNames))
        case SchemaKind.Alias | SchemaKind.Skip => ()
      }
    }

    SchemaConversionResult(
      models = models.result(),
      enums = enums.result(),
      unions = unions.result(),
    )
  }

  private def convertObjectSchema(
    name: String,
    schema: Schema,
    fields: Seq[ClassifiedField],
  ): (Option[Model], Seq[Enum]) = {
    val converted = fields.flatMap(convertClassifiedField)
    val model = Model(
      name = sn(name),
      plural = sn(name) + "s",
      description = schema.description,
      deprecation = None,
      fields = converted.map(_._1),
      attributes = Seq.empty,
      interfaces = Seq.empty,
    )
    (Some(model), converted.flatMap(_._2))
  }

  private def convertTopLevelEnum(name: String, schema: Schema): Enum =
    Enum(
      name = sn(name),
      plural = sn(name) + "s",
      description = schema.description,
      deprecation = None,
      values = enumStrings(schema).map(v => EnumValue(name = v)),
      attributes = Seq.empty,
    )

  private def convertArraySchema(name: String, schema: Schema): Model = {
    val itemType = schema.items
      .map {
        case s: Schema if s.$ref.isDefined => refName(s.$ref.get)
        case s: Schema =>
          simpleType(s).map(_.name).getOrElse {
            System.err.println(s"Warning: could not resolve array item type for schema '$name'; defaulting to string")
            ScalarType.StringType.name
          }
        case _ =>
          System.err.println(s"Warning: unrecognised array items schema type for '$name'; defaulting to string")
          ScalarType.StringType.name
      }
      .getOrElse {
        System.err.println(s"Warning: array schema '$name' has no items definition; defaulting to string")
        ScalarType.StringType.name
      }

    Model(
      name = sn(name),
      plural = sn(name) + "s",
      description = schema.description,
      deprecation = None,
      fields = Seq(
        Field(
          name = sn(name) + "_items",
          `type` = sn(arrayType(itemType)),
          description = schema.description,
          deprecation = None,
          default = None,
          required = true,
          minimum = None,
          maximum = None,
          example = None,
          attributes = Seq.empty,
          annotations = Seq.empty,
        ),
      ),
      attributes = Seq.empty,
      interfaces = Seq.empty,
    )
  }

  private def convertClassifiedField(cf: ClassifiedField): Option[(Field, Seq[Enum])] = {
    if (cf.kind.isEmpty)
      System.err.println(s"Warning: could not classify field '${cf.fieldName}' in schema '${cf.schemaName}'; field will be omitted")
    cf.kind.map {
      case FieldKind.Ref(target) =>
        (makeField(cf, resolve(target)), Nil)

      case FieldKind.AllOfRef(target) =>
        (makeField(cf, resolve(target)), Nil)

      case FieldKind.Number =>
        (makeField(cf, ScalarType.DoubleType.name), Nil)

      case FieldKind.ArrayRef(target) =>
        (makeField(cf, arrayType(resolve(target))), Nil)

      case FieldKind.ArrayEnum(itemsSchema) =>
        val (field, enumDef) = inlineEnum(cf, itemsSchema)
        (field, Seq(enumDef))

      case FieldKind.InlineEnum(enumSchema) =>
        val (field, enumDef) = inlineEnum(cf, enumSchema)
        (field, Seq(enumDef))

      case FieldKind.MapType(valueType) =>
        (makeField(cf, NamingUtils.mapType(resolve(valueType))), Nil)

      case FieldKind.ArraySimple(scalarType) =>
        (makeField(cf, arrayType(scalarType.name)), Nil)

      case FieldKind.Primitive(scalarType) =>
        (makeField(cf, scalarType.name), Nil)

      case FieldKind.DefaultedString =>
        (makeField(cf, ScalarType.StringType.name), Nil)
    }
  }

  private def inlineEnum(cf: ClassifiedField, schema: Schema): (Field, Enum) = {
    val enumName = cf.schemaName + "_" + cf.fieldName + "_enum"
    val enumDef = Enum(
      name = sn(enumName),
      plural = sn(enumName) + "s",
      description = None,
      deprecation = None,
      values = enumStrings(schema).map(v =>
        EnumValue(
          name = NamingUtils.sanitizeEnumName(v),
          description = None,
          deprecation = None,
          attributes = Seq.empty,
          value = None,
        ),
      ),
      attributes = Seq.empty,
    )
    (makeField(cf, enumName), enumDef)
  }

  private def makeField(cf: ClassifiedField, typeName: String): Field =
    SchemaConverter.makeField(
      cf.fieldName,
      sn(typeName),
      cf.description,
      cf.required,
      cf.minimum,
      cf.maximum,
      cf.annotations,
    )

  private def convertUnionSchema(name: String, schema: Schema, skippedNames: Set[String]): Union = {
    val refs = collectRefs(unionMembers(schema)).filterNot(skippedNames.contains)
    val discriminatorName = schema.discriminator.map(_.propertyName)
    val mapping = schema.discriminator.flatMap(_.mapping).getOrElse(ListMap.empty)

    val refToValue: Map[String, String] = mapping.map { case (value, ref) =>
      refName(ref) -> value
    }.toMap

    val types = refs.map { typeName =>
      val resolved = resolve(typeName)
      UnionType(
        `type` = sn(resolved),
        description = None,
        deprecation = None,
        attributes = Seq.empty,
        default = None,
        discriminatorValue = refToValue.get(typeName),
      )
    }

    Union(
      name = sn(name),
      plural = sn(name) + "s",
      discriminator = discriminatorName,
      description = schema.description,
      deprecation = None,
      types = types,
      attributes = Seq.empty,
      interfaces = Seq.empty,
    )
  }

  private def sn(str: String): String = uniqueSnakeCase(str, config)
  private def resolve(name: String): String = resolveReference(name, modelReferences) match {
    case Right(resolved) => resolved
    case Left(err) =>
      System.err.println(s"Warning: $err")
      name
  }
}

case class SchemaConversionResult(
  models: Seq[Model],
  enums: Seq[Enum],
  unions: Seq[Union],
)

object SchemaConverter {

  def makeField(
    name: String,
    typeName: String,
    description: Option[String],
    required: Boolean,
    minimum: Option[Long] = None,
    maximum: Option[Long] = None,
    annotations: SchemaClassifier.FieldAnnotations = SchemaClassifier.FieldAnnotations.empty,
  ): Field = {
    val attributes = annotations.ignoredFormat.toSeq.map { fmt =>
      Attribute(
        name = "openapi_format",
        value = Json.obj("format" -> fmt),
      )
    }
    Field(
      name = name,
      `type` = typeName,
      description = description,
      deprecation = annotations.deprecation,
      default = annotations.default,
      required = required,
      minimum = minimum,
      maximum = maximum,
      example = annotations.example,
      attributes = attributes,
      annotations = Seq.empty,
    )
  }

  def enumStrings(s: Schema): Seq[String] =
    s.`enum`.toList.flatten.collect { case ExampleSingleValue(v) => v.toString }

  private val FormatMap: Map[String, ScalarType] = Map(
    "int64" -> ScalarType.LongType,
    "float" -> ScalarType.FloatType,
    "double" -> ScalarType.DoubleType,
    "decimal" -> ScalarType.DecimalType,
    "uuid" -> ScalarType.UuidType,
    "date" -> ScalarType.DateIso8601Type,
    "date-time" -> ScalarType.DateTimeIso8601Type,
  )

  def simpleType(s: Schema): Option[ScalarType] = {
    lazy val fromFormat: Option[ScalarType] = s.format.flatMap(FormatMap.get)

    lazy val fromType: Option[ScalarType] = Seq(
      SchemaType.Boolean -> ScalarType.BooleanType,
      SchemaType.String -> ScalarType.StringType,
      SchemaType.Integer -> ScalarType.IntegerType,
      SchemaType.Number -> ScalarType.DecimalType,
    ).collectFirst { case (st, scalarType) if hasType(s, st) => scalarType }

    fromFormat.orElse(fromType)
  }

  def ignoredFormat(s: Schema): Option[String] =
    s.format.filterNot(FormatMap.contains)

  private def hasType(s: Schema, st: SchemaType): Boolean =
    SchemaResolver.hasType(s, st)
}
