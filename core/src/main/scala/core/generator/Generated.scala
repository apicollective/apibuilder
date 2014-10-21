package com.gilt.apidocgenerator.models {
  case class Enum(
    name: String,
    description: scala.Option[String] = None,
    values: scala.collection.Seq[com.gilt.apidocgenerator.models.EnumValue]
  )

  case class EnumValue(
    name: String,
    description: scala.Option[String] = None
  )

  case class Error(
    code: String,
    message: String
  )

  case class Field(
    name: String,
    datatype: com.gilt.apidocgenerator.models.Type,
    description: scala.Option[String] = None,
    required: Boolean,
    default: scala.Option[String] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  )

  /**
   * The generator metadata.
   */
  case class Generator(
    key: String,
    name: String,
    language: scala.Option[String] = None,
    description: scala.Option[String] = None
  )

  case class Header(
    name: String,
    headertype: com.gilt.apidocgenerator.models.HeaderType,
    headertypeValue: scala.Option[String] = None,
    description: scala.Option[String] = None,
    required: Boolean,
    multiple: Boolean,
    default: scala.Option[String] = None
  )

  /**
   * The result of invoking a generator.
   */
  case class Invocation(
    source: String
  )

  case class Model(
    name: String,
    plural: String,
    description: scala.Option[String] = None,
    fields: scala.collection.Seq[com.gilt.apidocgenerator.models.Field]
  )

  case class Operation(
    model: com.gilt.apidocgenerator.models.Model,
    method: String,
    path: String,
    description: scala.Option[String] = None,
    body: scala.Option[com.gilt.apidocgenerator.models.Type] = None,
    parameters: scala.collection.Seq[com.gilt.apidocgenerator.models.Parameter],
    responses: scala.collection.Seq[com.gilt.apidocgenerator.models.Response]
  )

  case class Parameter(
    name: String,
    datatype: com.gilt.apidocgenerator.models.Type,
    location: com.gilt.apidocgenerator.models.ParameterLocation,
    description: scala.Option[String] = None,
    required: Boolean,
    default: scala.Option[String] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  )

  case class Resource(
    model: com.gilt.apidocgenerator.models.Model,
    path: String,
    operations: scala.collection.Seq[com.gilt.apidocgenerator.models.Operation]
  )

  case class Response(
    code: Int,
    datatype: com.gilt.apidocgenerator.models.Type
  )

  /**
   * Description of a service
   */
  case class ServiceDescription(
    enums: scala.collection.Seq[com.gilt.apidocgenerator.models.Enum],
    models: scala.collection.Seq[com.gilt.apidocgenerator.models.Model],
    headers: scala.collection.Seq[com.gilt.apidocgenerator.models.Header],
    resources: scala.collection.Seq[com.gilt.apidocgenerator.models.Resource],
    baseUrl: scala.Option[String] = None,
    name: String,
    packageName: scala.Option[String] = None,
    description: scala.Option[String] = None,
    userAgent: scala.Option[String] = None
  )

  case class Type(
    kind: com.gilt.apidocgenerator.models.TypeKind,
    name: String,
    multiple: Boolean
  )

  sealed trait HeaderType

  object HeaderType {

    case object String extends HeaderType { override def toString = "string" }
    case object Enum extends HeaderType { override def toString = "enum" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends HeaderType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(String, Enum)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): HeaderType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[HeaderType] = byName.get(value)

  }

  sealed trait ParameterLocation

  object ParameterLocation {

    case object Path extends ParameterLocation { override def toString = "path" }
    case object Query extends ParameterLocation { override def toString = "query" }
    case object Form extends ParameterLocation { override def toString = "form" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends ParameterLocation

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Path, Query, Form)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): ParameterLocation = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[ParameterLocation] = byName.get(value)

  }

  sealed trait TypeKind

  object TypeKind {

    case object Primitive extends TypeKind { override def toString = "primitive" }
    case object Model extends TypeKind { override def toString = "model" }
    case object Enum extends TypeKind { override def toString = "enum" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends TypeKind

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Primitive, Model, Enum)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): TypeKind = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[TypeKind] = byName.get(value)

  }
}
