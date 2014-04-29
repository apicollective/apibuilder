package core.generator.scala

import core.{ Datatype, Field, ServiceDescription, Resource, Text }

object CaseClassGenerator {
  def apply(service: ServiceDescription) = new CaseClassGenerator(service).generate
}

class CaseClassGenerator(service: ServiceDescription) {
  def generate: Map[String, String] = {
    service.resources.map { resource =>
      val className = Text.singular(Text.underscoreToInitCap(resource.name))
      className -> generateResource(className, resource)
    }.toMap
  }

  def generateResource(className: String, resource: Resource): String = {
    val scalaFields = generateFields(resource.fields)
    val importSource = scalaFields.flatMap { scalaField =>
      scalaField.imports.map { "import " + _ }
    }.sorted.distinct.mkString("\n")
    val fieldSource = scalaFields.map { scalaField =>
s"  ${scalaField.src}"
    }.mkString(",\n")
    val classSource = s"""
case class $className(
$fieldSource
)
"""
    importSource + classSource
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
