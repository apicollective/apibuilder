package me.apidoc.avro

import org.apache.avro.Schema
import scala.collection.JavaConversions._
import com.gilt.apidoc.spec.v0.models._

object Apidoc {

  case class Type(
    name: String,
    required: Boolean = true
  )

  object Field {
    def apply(field: Schema.Field): Field = {
      val t = Apidoc.getType(field.schema)
      val default = if (field.defaultValue() == null || field.defaultValue().isNull()) {
        None
      } else {
        Some(field.defaultValue().toString())
      }

      com.gilt.apidoc.spec.v0.models.Field(
        name = Util.formatName(field.name),
        `type` = t.name,
        description = Util.toOption(field.doc),
        default = default,
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
              Type(Util.formatName(schema.getName))
            }
          }
          case types => {
            Type(Util.formatName(schema.getName))
          }
        }
      }
      case SchemaType.Record => Type(Util.formatName(schema.getName))
    }
  }

}
