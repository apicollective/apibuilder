package core.generator.scala

import core.{ Datatype, Field, ServiceDescription, Resource, Text }

case class ScalaCaseClass(name: String, fields: Seq[ScalaField], resource: Resource)
extends Source {
  val imports = fields.flatMap(_.imports)

  override val src: String = {
    val fieldSource = fields.map { field =>
      s"  ${field.src}"
    }.mkString(",\n")
s"""
case class $name(
$fieldSource
)
"""
  }
}

object CaseClassGenerator {
  def apply(service: ServiceDescription) = new CaseClassGenerator(service).generate
}

class CaseClassGenerator(service: ServiceDescription) {
  def generate: Seq[ScalaCaseClass] = {
    service.resources.map { resource =>
      val className = Text.singular(Text.underscoreToInitCap(resource.name))
      generateResource(className, resource)
    }
  }

  def generateResource(className: String, resource: Resource): ScalaCaseClass = {
    val scalaFields = generateFields(resource.fields)
    new ScalaCaseClass(className, scalaFields, resource)
  }

  def generateFields(fields: Seq[Field]): Seq[ScalaField] = {
    fields.map { field =>
      FieldGenerator(field)
    }
  }

  def scalaFieldName(fieldName: String): String = {
    """_(\w)""".r.replaceAllIn(
      fieldName,
      m => m.group(1).toUpperCase
    )
  }

  def scalaTypeName(typeName: String, format: Option[String]): String = {
    typeName match {
      case "string" => format.map {
        case "date-time" => "org.joda.time.DateTime"
        case "uuid" => "UUID"
      }.getOrElse {
        "String"
      }
      case "long" => "Long"
      case "integer" => "Int"
      case "decimal" => "BigDecimal"
      case _ => Text.underscoreToInitCap(typeName)
    }
  }
}
