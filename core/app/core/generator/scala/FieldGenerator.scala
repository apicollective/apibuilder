package core.generator.scala

import core.{ Datatype, Field, Text }
import Datatype._

case class ScalaField(name: String, typeName: String, imports: Seq[String])
extends Source {
  override val src: String = s"$name: $typeName"
}

object FieldGenerator {
  def apply(field: Field): ScalaField = {
    val name = Text.underscoreToCamelCase(field.name)
    val typeName = field.dataType match {
      case String => field.format.map {
        case "date-time" => "DateTime"
        case "uuid" => "UUID"
      }.getOrElse {
        "String"
      }
      case Integer => "Int"
      case Long => "Long"
      case Boolean => "Boolean"
      case Decimal => "BigDecimal"
      case dt => Text.underscoreToInitCap(dt.name)
    }
    val imports = typeName match {
      case "DateTime" => "org.joda.time.DateTime" :: Nil
      case "UUID" => "java.util.UUID" :: Nil
      case _ => Nil
    }
    new ScalaField(name, typeName, imports)
  }
}
