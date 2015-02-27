package me.apidoc.avro

import java.io.File
import org.apache.avro.{Protocol, Schema}
import org.apache.avro.compiler.idl.Idl
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}

import lib.Text
import com.gilt.apidoc.spec.v0.models._

private[avro] case class Builder() {

  //private val enums = scala.collection.mutable.Map[String, JsValue]()
  private val enumBuilder = scala.collection.mutable.ListBuffer[Enum]()
  private val models = scala.collection.mutable.Map[String, JsValue]()
  private val unions = scala.collection.mutable.Map[String, JsValue]()

  def enums(): Seq[Enum] = enumBuilder.toSeq

  def toApiJson(
    name: String
  ): JsValue = {
    JsObject(
      Seq(
        Some("name" -> JsString(name)),
        enums.toList match {
          case Nil => None
          case data => Some("enums" -> JsObject(data))
        },
        models.toList match {
          case Nil => None
          case data => Some("models" -> JsObject(data))
        }
      ).flatten
    )
  }

  def addModel(name: String, description: Option[String], fields: Seq[Apidoc.Field]) {
    models += (Util.formatName(name) ->
      JsObject(
        Seq(
          description.map { v => "description" -> JsString(v) },
          Some("fields" -> JsArray(fields.map(_.jsValue)))
        ).flatten
      )
    )
  }

  def addUnion(name: String, description: Option[String], types: Seq[Apidoc.EnumValue]) {
    unions += (Util.formatName(name) ->
      JsObject(
        Seq(
          description.map { v => "description" -> JsString(v) },
          Some("types" -> JsArray(types.map(_.jsValue)))
        ).flatten
      )
    )
  }

  /**
    * Avro supports fixed types in the schema which is a way of
    * extending the type system. apidoc doesn't have support for that;
    * the best we can do is to create a model representing the fixed
    * element.
    */
  def addFixed(name: String, description: Option[String], size: Int) {
    addModel(
      name = name,
      description = description,
      fields = Seq(
        Apidoc.Field(name = "value", typeName = "string", maximum = Some(size))
      )
    )
  }

  def addEnum(name: String, description: Option[String], values: Seq[String]) {
    enumsBuilder += Enum(
      name = name,
      plural = Text.pluralize(name)
    )
  }
}

case class Parser() {

  val builder = Builder()

  def parse(path: String) {
    println(s"parse($path)")

    val protocol = parseProtocol(path)
    println(s"protocol name[${protocol.getName}] namespace[${protocol.getNamespace}]")

    protocol.getTypes.foreach { schema =>
      parseSchema(schema)
    }

    println(
      builder.toApiJson(
        name = protocol.getName
      )
    )

  }


  private def parseSchema(schema: Schema) {
    SchemaType.fromAvro(schema.getType) match {
      case None => sys.error(s"Unsupported schema type[${schema.getType}]")
      case Some(st) => {
        st match {
          case SchemaType.Union => parseUnion(schema)
          case SchemaType.Record => parseRecord(schema)
          case SchemaType.Enum => parseEnum(schema)
          case SchemaType.Fixed => parseFixed(schema)
          case SchemaType.Array | SchemaType.Boolean | SchemaType.Bytes | SchemaType.Double | SchemaType.Float | SchemaType.Int | SchemaType.Long | SchemaType.Map | SchemaType.String | SchemaType.Null => {
            sys.error(s"Unexpected avro type[$st]")
          }
        }
      }
    }
  }

  private def parseFixed(schema: Schema) {
    builder.addFixed(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      size = schema.getFixedSize()
    )
  }

  private def parseEnum(schema: Schema) {
    builder.addEnum(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      values = schema.getEnumSymbols
    )
  }

  private def parseRecord(schema: Schema) {
    builder.addModel(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      fields = schema.getFields.map(Apidoc.Field(_))
    )
  }

  private def parseUnion(schema: Schema) {
    val types: Seq[Apidoc.EnumValue] = Nil // TODO: schema.getTypes.map(Apidoc.EnumValue(_))
    builder.addUnion(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      types = types
    )
  }

  private def parseProtocol(
    path: String
  ): Protocol = {
    if (path.endsWith(".avdl")) {
      new Idl(new File(path)).CompilationUnit()

    } else if (path.endsWith(".avpr")) {
      Protocol.parse(new java.io.File(path))

    } else {
      sys.error("Unrecognized file extension for path[$path]")
    }
  }

}
