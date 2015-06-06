package me.apidoc.avro

import org.apache.avro.Schema
import scala.collection.JavaConversions._
import com.bryzek.apidoc.spec.v0.models._

object Apidoc {

  sealed trait Type {
    def name: String
    def required: Boolean
  }

  case class SimpleType(
    name: String,
    required: Boolean = true
  ) extends Type

  case class UnionType(
    names: Seq[String],
    required: Boolean
  ) extends Type {

    override val name = names.mkString("_or_")

  }

  object Field {
    def apply(field: Schema.Field): Field = {
      val t = Apidoc.getType(field.schema)

      val default = if (field.defaultValue() == null || field.defaultValue().isNull()) {
        None
      } else {
        Some(field.defaultValue().toString())
      }

      com.bryzek.apidoc.spec.v0.models.Field(
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
      case SchemaType.Array => SimpleType("[%s]".format(getType(schema.getElementType).name))
      case SchemaType.Boolean => SimpleType("boolean")
      case SchemaType.Bytes => sys.error("apidoc does not support bytes type")
      case SchemaType.Double => SimpleType("double")
      case SchemaType.Enum => SimpleType(Util.formatName(schema.getName))
      case SchemaType.Fixed => SimpleType(Util.formatName(schema.getName))
      case SchemaType.Float => SimpleType("double")
      case SchemaType.Int => SimpleType("integer")
      case SchemaType.Long => SimpleType("long")
      case SchemaType.Map => SimpleType("map[%s]".format(getType(schema.getValueType).name))
      case SchemaType.Null => SimpleType("unit")
      case SchemaType.String => SimpleType("string")
      case SchemaType.Record => SimpleType(Util.formatName(schema.getName))
      case SchemaType.Union => unionType(schema.getTypes)
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
