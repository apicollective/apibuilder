package core

import Text._

object ScalaUtil {
  def textToComment(text: String) = {
    val lines = text.split("\n")
    lines.mkString("/**\n * ", "\n * ", "\n */\n")
  }

  def fieldsToArgList(fields: Seq[ScalaField]) = {
    fields.map(_.src.indent).mkString("\n", ",\n", "\n")
  }
}

import ScalaUtil._

class ScalaServiceDescription(serviceDescription: ServiceDescription)
{
  val name = safeName(serviceDescription.name)

  val description = serviceDescription.description.map(textToComment).getOrElse("")

  val resources = serviceDescription.resources.map { resource =>
    new ScalaResource(resource)
  }
}

class ScalaResource(resource: Resource)
{
  val name = singular(underscoreToInitCap(resource.name))

  val path = resource.path

  val description = resource.description.map(textToComment).getOrElse("")

  val fields = resource.fields.map { field =>
    new ScalaField(field)
  }.sorted

  val argList = fieldsToArgList(fields)

  val operations = resource.operations.map { operation =>
    new ScalaOperation(operation)
  }
}

class ScalaOperation(operation: Operation)
{
  val method = operation.method

  val path = operation.path

  val description = operation.description.map(textToComment).getOrElse("")

  val parameters = operation.parameters.map { new ScalaField(_) }.sorted

  val name = method.toLowerCase + path.map(safeName).getOrElse("").capitalize

  val argList = fieldsToArgList(parameters)
}

class ScalaField(field: Field) extends Source with Ordered[ScalaField] {
  def name: String = snakeToCamelCase(field.name)

  def originalName: String = field.name

  def datatype: ScalaDataType = new ScalaDataType(field.datatype, field.format)

  def description: String = field.description.map(textToComment).getOrElse("")

  def isOption: Boolean = !field.required || field.default.nonEmpty

  def typeName: String = if (isOption) s"Option[${datatype.name}]" else datatype.name

  override def src: String = {
    val decl = s"$description$name: $typeName"
    if (isOption) decl + " = None" else decl
  }

  // we just want to make sure that fields with defaults
  // always come after those without, so that argument lists will
  // be valid. otherwise, preserve existing order
  override def compare(that: ScalaField): Int = {
    if (isOption) {
      if (that.isOption) {
        0
      } else {
        1
      }
    } else if (that.isOption) {
      -1
    } else {
      0
    }
  }
}

class ScalaDataType(datatype: Datatype, format: Option[Format]) extends Source {
  import Datatype._
  import Format._
  val name = datatype match {
    case String => format.map {
      case Uuid => "UUID"
      case DateTimeIso8601 => "DateTime"
    }.getOrElse {
      "String"
    }
    case Integer => "Int"
    case Long => "Long"
    case Boolean => "Boolean"
    case Decimal => "BigDecimal"
    case Unit => "Unit"
  }

  override val src: String = datatype.name
}
