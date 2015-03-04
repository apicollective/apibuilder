package me.apidoc.avro

import java.io.File
import org.apache.avro.{Protocol, Schema}
import org.apache.avro.compiler.idl.Idl
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}

import lib.Text
import com.gilt.apidoc.spec.v0.models._

private[avro] case class Builder() {

  private val enumsBuilder = scala.collection.mutable.ListBuffer[Enum]()
  private val modelsBuilder = scala.collection.mutable.ListBuffer[Model]()
  private val unionsBuilder = scala.collection.mutable.ListBuffer[Union]()

  def toService(
    name: String,
    namespace: Option[String] = None,
    orgKey: String,
    applicationKey: String,
    version: String
  ): Service = {
    Service(
      name = name,
      baseUrl = None,
      description = None,
      namespace = namespace.getOrElse(s"$orgKey.$applicationKey"),
      organization = Organization(key = orgKey),
      application = Application(key = applicationKey),
      version = version,
      enums = enumsBuilder,
      unions = unionsBuilder,
      models = modelsBuilder,
      imports = Nil,
      headers = Nil,
      resources = Nil
    )
  }

  def addModel(
    name: String,
    description: Option[String],
    fields: Seq[Field]
  ) {
    modelsBuilder += Model(
      name = name,
      plural = Text.pluralize(name),
      description = description,
      fields = fields
    )
  }

  def addUnion(name: String, description: Option[String], types: Seq[UnionType]) {
    sys.error(name)
    unionsBuilder += Union(
      name = name,
      plural = Text.pluralize(name),
      description = description,
      types = types
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
        Field(
          name = "value",
          `type` = "string",
          required = true,
          description = Some(s"Avro fixed type with size[$size]"),
          maximum = Some(size)
        )
      )
    )
  }

  def addEnum(name: String, description: Option[String], values: Seq[String]) {
    enumsBuilder += Enum(
      name = Util.formatName(name),
      plural = Text.pluralize(name),
      description = description,
      values = values.map { n => 
        EnumValue(
          name = Util.formatName(n)
        )
      }
    )
  }

}

case class Parser() {

  val builder = Builder()

  def parse(path: String): Service = {
    println(s"parse($path)")

    val protocol = parseProtocol(path)
    println(s"protocol name[${protocol.getName}] namespace[${protocol.getNamespace}]")

    protocol.getTypes.foreach { schema =>
      parseSchema(schema)
    }

    builder.toService(
      name = protocol.getName,
      namespace = Util.toOption(protocol.getNamespace),
      orgKey = "gilt",
      applicationKey = "test",
      version = "0.0.1-dev"
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
      fields = schema.getFields.map(Apidoc.Field.apply(_))
    )
  }

  private def parseUnion(schema: Schema) {
    // TODO
    val types: Seq[UnionType] = Nil
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
