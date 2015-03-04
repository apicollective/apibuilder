package me.apidoc.avro

import org.apache.avro.Schema
import scala.collection.JavaConversions._
import com.gilt.apidoc.spec.v0.models._

object Apidoc {

  case class Type(
    name: String,
    required: Boolean = true
  )

  case class UnionType(
    names: Seq[String],
    required: Boolean
  ) {

    val `type`: Type = {
      names match {
        case Nil => sys.error("union must have at least 1 type")
        case name :: Nil => Type(name = name, required = required)
        case names => Type(name = names.mkString("_or_"), required = required)
      }
    }

  }

  object Field {
    def apply(field: Schema.Field): Field = {
      val t = Apidoc.getType(field.schema)
      println(s"FIELD[${field.name}] type[$t]")
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
      case SchemaType.Record => Type(Util.formatName(schema.getName))
      case SchemaType.Union => unionType(schema.getTypes).`type`
    }
  }

  /**
    * Manufacture a union type name based on the provided Types.
    */
  def unionType(types: Seq[Schema]): UnionType = {
    UnionType(
      names = types.filter(t => !isNullType(t.getType)).map(t => Util.formatName(t.getName)),
      required = !hasNullType(types)
    )
  }

  private def hasNullType(types: Seq[Schema]): Boolean = {
    types.find(t => isNullType(t.getType)) match {
      case None => false
      case Some(_) => true
    }
  }

  private def isNullType(t: Schema.Type): Boolean = {
    SchemaType.fromAvro(t) == Some(SchemaType.Null)
  }

}
