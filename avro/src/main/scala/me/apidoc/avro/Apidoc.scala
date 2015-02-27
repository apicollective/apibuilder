package me.apidoc.avro

import org.apache.avro.Schema
import scala.collection.JavaConversions._
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString}

object Apidoc {

  case class Type(
    name: String,
    required: Boolean = true
  )

  case class EnumValue(
    name: String,
    description: Option[String] = None
  ) {

    val jsValue = JsObject(
      Seq(
        Some("name" -> JsString(name)),
        description.map { v => "description" -> JsString(v) }
      ).flatten
    )

  }

/*
  object EnumValue {
    def apply(schema: Schema): EnumValue = {
      val t = Apidoc.getType(schema)
      EnumValue(
        name = Util.formatName(schema.name),
        description = Util.toOption(schema.doc)
      )
    }
  }
 */

  case class Field(
    name: String,
    typeName: String,
    description: Option[String] = None,
    required: Boolean = true,
    minimum: Option[Long] = None,
    maximum: Option[Long] = None
  ) {

    val jsValue = JsObject(
      Seq(
        Some("name" -> JsString(name)),
        required match {
          case true => None
          case false => Some("required" -> JsBoolean(false))
        },
        Some("type" -> JsString(typeName)),
        description.map { v => "description" -> JsString(v) },
        minimum.map { v => "minimum" -> JsNumber(v) },
        maximum.map { v => "maximum" -> JsNumber(v) }
      ).flatten
    )

  }

  object Field {
    def apply(field: Schema.Field): Field = {
      val t = Apidoc.getType(field.schema)
      Field(
        name = Util.formatName(field.name),
        typeName = t.name,
        description = Util.toOption(field.doc),
        required = t.required
      )
    }
  }

  def getType(schema: Schema): Type = {
    SchemaType.fromAvro(schema.getType).getOrElse {
      sys.error(s"Unsupported schema type[${schema.getType}]")
    } match {
      case SchemaType.Array => Type("[%s]".format(getType(schema.getElementType).name))
      case SchemaType.Boolean => Type("boolean")
      case SchemaType.Bytes => sys.error("apidoc does not support bytes type")
      case SchemaType.Double => Type("double")
      case SchemaType.Enum => Type(Util.formatName(schema.getName))
      case SchemaType.Fixed => Type(Util.formatName(schema.getName))
      case SchemaType.Float => Type("double")
      case SchemaType.Int => Type("integer")
      case SchemaType.Long => Type("long")
      case SchemaType.Map => Type("map[%s]".format(getType(schema.getValueType).name))
      case SchemaType.Null => Type("unit")
      case SchemaType.String => Type("string")
      case SchemaType.Union => {
        schema.getTypes.toList match {
          case Nil => sys.error("union must have at least 1 type")
          case t :: Nil => getType(t)
          case t1 :: t2 :: Nil => {
            if (SchemaType.fromAvro(t1.getType) == Some(SchemaType.Null)) {
              getType(t2).copy(required = false)

            } else if (SchemaType.fromAvro(t2.getType) == Some(SchemaType.Null)) {
              getType(t1).copy(required = false)

            } else {
              sys.error("apidoc does not support union types w/ more then 1 non null type: " + Seq(t1, t2).map(_.getType).mkString(", "))
            }
          }
          case types => {
            sys.error("apidoc does not support union types w/ more then 1 non null type: " + types.map(_.getType).mkString(", "))
          }
        }
      }
      case SchemaType.Record => Type(Util.formatName(schema.getName))
    }
  }

}
