package me.apidoc.avro

import lib.{ServiceConfiguration, UrlKey}
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import org.apache.avro.{Protocol, Schema}
import org.apache.avro.compiler.idl.Idl
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}
import java.util.UUID

import lib.Text
import io.apibuilder.spec.v0.models._

private[avro] case class Builder() {

  private val enumsBuilder = scala.collection.mutable.ListBuffer[Enum]()
  private val modelsBuilder = scala.collection.mutable.ListBuffer[Model]()
  private val unionsBuilder = scala.collection.mutable.ListBuffer[Union]()

  def toService(
    config: ServiceConfiguration,
    name: String,
    namespace: Option[String] = None,
    applicationKey: String
  ): Service = {
    Service(
      apidoc = io.apibuilder.spec.v0.models.Apidoc(version = io.apibuilder.spec.v0.Constants.Version),
      name = name,
      info = Info(license = None, contact = None),
      baseUrl = None,
      description = None,
      namespace = namespace.getOrElse(config.applicationNamespace(applicationKey)),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
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
      name = Util.formatName(name),
      plural = Text.pluralize(name),
      description = description,
      fields = fields
    )
  }

  def addUnion(name: String, description: Option[String], types: Seq[UnionType]) {
    unionsBuilder += Union(
      name = Util.formatName(name),
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
      name = Util.formatName(name),
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

case class Parser(config: ServiceConfiguration) {

  val builder = Builder()

  def parse(
    path: File
  ): Service = {
    println(s"parse($path)")
    parse(parseFile(path))
  }

  def parseString(
    contents: String
  ): Service = {
    val tmpPath = "/tmp/apidoc.tmp." + UUID.randomUUID.toString + ".avdl"
    writeToFile(tmpPath, contents)
    parse(new File(tmpPath))
  }

  private def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def parse(
    protocol: Protocol
  ): Service = {
    println(s"protocol name[${protocol.getName}] namespace[${protocol.getNamespace}]")

    protocol.getTypes.foreach { schema =>
      parseSchema(schema)
    }

    builder.toService(
      config = config,
      name = protocol.getName,
      namespace = Util.toOption(protocol.getNamespace),
      applicationKey = UrlKey.generate(protocol.getName)
    )

  }


  private def parseSchema(schema: Schema) {
    SchemaType.fromAvro(schema.getType) match {
      case None => sys.error(s"Unsupported schema type[${schema.getType}]")
      case Some(st) => {
        st match {
          case SchemaType.Union => {
            sys.error("Did not expect a top level union type")
          }
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
      name = Util.formatName(schema.getName),
      description = Util.toOption(schema.getDoc),
      fields = schema.getFields.map(Apidoc.Field.apply(_))
    )

    schema.getFields.foreach { f =>
      Apidoc.getType(f.schema()) match {
        case Apidoc.SimpleType(_, _) => {}
        case u: Apidoc.UnionType => {
          println(s"UNION[${u.name}: " + u.names.mkString(","))

          builder.addUnion(
            name = Util.formatName(u.name),
            description = None,
            types = u.names.map { n =>
              UnionType(n)
            }
          )
        }
      }
    }
  }

  private def parseFile(
    path: File
  ): Protocol = {
    if (path.toString.endsWith(".avdl")) {
      new Idl(path).CompilationUnit()

    } else if (path.toString.endsWith(".avpr")) {
      Protocol.parse(path)

    } else {
      sys.error(s"Unrecognized file extension for path[$path]")
    }
  }

}
