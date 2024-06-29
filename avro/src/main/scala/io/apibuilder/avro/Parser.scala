package io.apibuilder.avro

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

import io.apibuilder.spec.v0.models.{Application, Enum, EnumValue, Field, Info, Model, Organization, Service, Union, UnionType}
import lib.{ServiceConfiguration, Text, UrlKey}
import org.apache.avro.compiler.idl.Idl
import org.apache.avro.{Protocol, Schema}

import scala.jdk.CollectionConverters._

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
      name = name,
      info = Info(license = None, contact = None),
      baseUrl = None,
      description = None,
      namespace = namespace.getOrElse(config.applicationNamespace(applicationKey)),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
      enums = enumsBuilder.toSeq,
      unions = unionsBuilder.toSeq,
      models = modelsBuilder.toSeq,
      imports = Nil,
      headers = Nil,
      resources = Nil
    )
  }

  def addModel(
    name: String,
    description: Option[String],
    fields: Seq[Field]
  ): Unit = {
    modelsBuilder += Model(
      name = Util.formatName(name),
      plural = Text.pluralize(name),
      description = description,
      fields = fields
    )
  }

  def addUnion(name: String, description: Option[String], types: Seq[UnionType]): Unit = {
    unionsBuilder += Union(
      name = Util.formatName(name),
      plural = Text.pluralize(name),
      description = description,
      types = types
    )
  }

  /**
    * Avro supports fixed types in the schema which is a way of
    * extending the type system. API Builder doesn't have support for that;
    * the best we can do is to create a model representing the fixed
    * element.
    */
  def addFixed(name: String, description: Option[String], size: Int): Unit = {
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

  def addEnum(name: String, description: Option[String], values: Seq[String]): Unit = {
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

  private val builder: Builder = Builder()

  def parse(
    path: File
  ): Service = {
    parse(parseFile(path))
  }

  def parseString(
    contents: String
  ): Service = {
    val tmpPath = "/tmp/apidoc.tmp." + UUID.randomUUID.toString + ".avdl"
    writeToFile(tmpPath, contents)
    parse(new File(tmpPath))
  }

  private def writeToFile(path: String, contents: String): Unit = {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  def parse(
    protocol: Protocol
  ): Service = {
    protocol.getTypes.asScala.foreach { schema =>
      parseSchema(schema)
    }

    builder.toService(
      config = config,
      name = protocol.getName,
      namespace = Util.toOption(protocol.getNamespace),
      applicationKey = UrlKey.generate(protocol.getName)
    )

  }


  private def parseSchema(schema: Schema): Unit = {
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

  private def parseFixed(schema: Schema): Unit = {
    builder.addFixed(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      size = schema.getFixedSize
    )
  }

  private def parseEnum(schema: Schema): Unit = {
    builder.addEnum(
      name = schema.getName,
      description = Util.toOption(schema.getDoc),
      values = schema.getEnumSymbols.asScala.toSeq
    )
  }

  private def parseRecord(schema: Schema): Unit = {
    builder.addModel(
      name = Util.formatName(schema.getName),
      description = Util.toOption(schema.getDoc),
      fields = schema.getFields.asScala.map(ApiBuilder.Field.apply).toSeq
    )

    schema.getFields.asScala.foreach { f =>
      ApiBuilder.getType(f.schema()) match {
        case ApiBuilder.SimpleType(_, _) => {}
        case u: ApiBuilder.UnionType => {
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
