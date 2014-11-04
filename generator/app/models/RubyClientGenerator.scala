package models

import com.gilt.apidocgenerator.models._
import core._
import Text._
import core.generator.{GeneratorUtil, CodeGenerator, ScalaUtil}
import scala.collection.mutable.ListBuffer

object RubyUtil {

  def toClassName(
    name: String,
    multiple: Boolean = false
  ): String = {
    ScalaUtil.toClassName(name, multiple)
  }

  def toVariable(
    name: String,
    multiple: Boolean = false
  ): String = {
    Text.initLowerCase(Text.camelCaseToUnderscore(toClassName(name, multiple)))
  }

}

object RubyClientGenerator extends CodeGenerator {

  override def generate(sd: ServiceDescription): String = {
    new RubyClientGenerator(sd).generate
  }

  def generateEnum(enum: Enum): String = {
    val className = RubyUtil.toClassName(enum.name)
    val lines = ListBuffer[String]()
    lines.append(s"class $className")

    lines.append("")
    lines.append("  attr_reader :value")

    lines.append("")
    lines.append("  def initialize(value)")
    lines.append("    @value = HttpClient::Preconditions.assert_class('value', value, String)")
    lines.append("  end")

    lines.append("")
    lines.append(s"  # Returns the instance of ${className} for this value, creating a new instance for an unknown value")
    lines.append(s"  def $className.apply(value)")
    lines.append(s"    if value.instance_of?($className)")
    lines.append(s"      value")
    lines.append(s"    else")
    lines.append(s"      HttpClient::Preconditions.assert_class_or_nil('value', value, String)")
    lines.append(s"      value.nil? ? nil : (from_string(value) || $className.new(value))")
    lines.append(s"    end")
    lines.append(s"  end")
    lines.append("")
    lines.append(s"  # Returns the instance of $className for this value, or nil if not found")
    lines.append(s"  def $className.from_string(value)")
    lines.append("    HttpClient::Preconditions.assert_class('value', value, String)")
    lines.append(s"    $className.ALL.find { |v| v.value == value }")
    lines.append("  end")

    lines.append("")
    lines.append(s"  def $className.ALL") // Upper case to avoid naming conflict
    lines.append("    @@all ||= [" + enum.values.map(v => s"$className.${enumName(v.name)}").mkString(", ") + "]")
    lines.append("  end")

    lines.append("")
    enum.values.foreach { value =>
      val varName = enumName(value.name)
      value.description.foreach { desc =>
        lines.append(GeneratorUtil.formatComment(desc).indent(2))
      }
      lines.append(s"  def $className.$varName")
      lines.append(s"    @@_$varName ||= $className.new('${value.name}')")
      lines.append("  end")
      lines.append("")
    }

    lines.append("end")
    lines.mkString("\n")
  }

  def enumName(value: String): String = {
    if (value == value.toUpperCase) {
      Text.camelCaseToUnderscore(value.toLowerCase).split("_").map(Text.initLowerCase(_)).mkString("_")
    } else {
      Text.camelCaseToUnderscore(value).split("_").map(Text.initLowerCase(_)).mkString("_")
    }
  }

  def apply(sd: ServiceDescription, userAgent: String): RubyClientGenerator = RubyClientGenerator(sd)

}

/**
 * Generates a Ruby Client file based on the service description
 * from api.json
 */
case class RubyClientGenerator(service: ServiceDescription) {
  private val moduleName = RubyUtil.toClassName(service.name)

  def generate(): String = {
    RubyHttpClient.require +
    "\n\n" +
    service.description.map { desc => GeneratorUtil.formatComment(desc) + "\n" }.getOrElse("") +
    s"module ${moduleName}\n" +
    generateClient() +
    "\n\n  module Clients\n\n" +
    service.resources.map { res => generateClientForResource(res) }.mkString("\n\n") +
    "\n\n  end" +
    "\n\n  module Models\n" +
    service.enums.map { RubyClientGenerator.generateEnum(_) }.mkString("\n\n").indent(4) + "\n\n" +
    service.models.map { generateModel(_) }.mkString("\n\n") +
    "\n\n  end\n\n  # ===== END OF SERVICE DEFINITION =====\n  " +
    RubyHttpClient.contents +
    "\nend"
  }

  case class Header(name: String, value: String)

  private def headers(): Seq[Header] = {
    service.headers.filter(!_.default.isEmpty).map { h =>
      Header(h.name, s"'${h.default.get}'")
    } ++ Seq(Header("User-Agent", "USER_AGENT"))
  }

  private def generateClient(): String = {
    val sb = ListBuffer[String]()
    val url = service.baseUrl

    val headerString = headers.map { h =>
      s"with_header('${h.name}', ${h.value})"
    }.mkString(".")

    sb.append(s"""
  class Client

    USER_AGENT = '${service.userAgent.getOrElse("unknown")}' unless defined?(USER_AGENT)

    def initialize(url, opts={})
      @url = HttpClient::Preconditions.assert_class('url', url, String)
      @authorization = HttpClient::Preconditions.assert_class_or_nil('authorization', opts.delete(:authorization), HttpClient::Authorization)
      HttpClient::Preconditions.assert_empty_opts(opts)
      HttpClient::Preconditions.check_state(url.match(/http.+/i), "URL[%s] must start with http" % url)
    end

    def request(path=nil)
      HttpClient::Preconditions.assert_class_or_nil('path', path, String)
      request = HttpClient::Request.new(URI.parse(@url + path.to_s)).$headerString

      if @authorization
        request.with_auth(@authorization)
      else
        request
      end
    end
""")

    sb.append(service.resources.map { resource =>
      val modelPlural = resource.model.plural
      val className = RubyUtil.toClassName(modelPlural)

      s"    def ${modelPlural}\n" +
      s"      @${modelPlural} ||= ${moduleName}::Clients::${className}.new(self)\n" +
      "    end"
    }.mkString("\n\n"))

    sb.append("  end")

    sb.mkString("\n")
  }

  def generateClientForResource(resource: Resource): String = {
    val className = RubyUtil.toClassName(resource.model.plural)

    val sb = ListBuffer[String]()
    sb.append(s"    class ${className}")
    sb.append("")
    sb.append("      def initialize(client)")
    sb.append(s"        @client = HttpClient::Preconditions.assert_class('client', client, ${moduleName}::Client)")
    sb.append("      end")

    resource.operations.foreach { op =>
      val pathParams = op.parameters.filter { p => p.location == ParameterLocation.Path }
      val queryParams = op.parameters.filter { p => p.location == ParameterLocation.Query }
      val formParams = op.parameters.filter { p => p.location == ParameterLocation.Form }

      val rubyPath = op.path.split("/").map { name =>
        if (name.startsWith(":")) {
          val varName = name.slice(1, name.length)
          val param = pathParams.find(_.name == varName).getOrElse {
            sys.error(s"Could not find path parameter named[$varName]")
          }
          param.`type` match {
            case TypeInstance(TypeContainer.Singleton, Type.Primitive(pt)) => asString(name, pt)
            case TypeInstance(TypeContainer.Singleton, Type.Model(name)) => sys.error("Models cannot be in the path")
            case TypeInstance(TypeContainer.Singleton, Type.Enum(name)) => s"#{${param.name}.value}"
            case TypeInstance(TypeContainer.List, _) => sys.error("Cannot have lists in the path")
            case TypeInstance(TypeContainer.Map, _) => sys.error("Cannot have maps in the path")
          }
        } else {
          name
        }
      }.mkString("/")

      val methodName = Text.camelCaseToUnderscore(
        GeneratorUtil.urlToMethodName(
          resource.model.plural, resource.path, op.method, op.path
        )
      ).toLowerCase

      val paramStrings = ListBuffer[String]()
      pathParams.map(_.name).foreach { n => paramStrings.append(n) }

      if (Util.isJsonDocumentMethod(op.method)) {
        op.body match {
          case None => paramStrings.append("hash")

          case Some(TypeInstance(TypeContainer.Singleton, Type.Primitive(pt))) => paramStrings.append(RubyUtil.toVariable("value", false))
          case Some(TypeInstance(_, Type.Primitive(pt))) => paramStrings.append(RubyUtil.toVariable("value", true))

          case Some(TypeInstance(TypeContainer.Singleton, Type.Model(name))) => paramStrings.append(RubyUtil.toVariable(name, false))
          case Some(TypeInstance(_, Type.Model(name))) => paramStrings.append(RubyUtil.toVariable(name, true))

          case Some(TypeInstance(TypeContainer.Singleton, Type.Enum(name))) => paramStrings.append(RubyUtil.toVariable(name, false))
          case Some(TypeInstance(_, Type.Enum(name))) => paramStrings.append(RubyUtil.toVariable(name, true))
        }
      }

      if (!queryParams.isEmpty) {
        paramStrings.append("incoming={}")
      }

      sb.append("")
      op.description.map { desc =>
        sb.append(GeneratorUtil.formatComment(desc, 6))
      }

      val paramCall = if (paramStrings.isEmpty) { "" } else { "(" + paramStrings.mkString(", ") + ")" }
      sb.append(s"      def ${methodName}$paramCall")

      pathParams.foreach { param =>
        val ti = parseTypeInstance(param.`type`)
        sb.append(s"        " + ti.assertMethod)
      }

      if (!queryParams.isEmpty) {
        val paramBuilder = ListBuffer[String]()

        queryParams.foreach { param =>
          paramBuilder.append(s":${param.name} => ${parseArgument(param)}")
        }

        sb.append("        opts = HttpClient::Helper.symbolize_keys(incoming)")
        sb.append("        query = {")
        sb.append("          " + paramBuilder.mkString(",\n          "))
        sb.append("        }.delete_if { |k, v| v.nil? }")
      }

      val requestBuilder = new StringBuilder()
      requestBuilder.append("@client.request(\"" + rubyPath + "\")")

      if (!queryParams.isEmpty) {
        requestBuilder.append(".with_query(query)")
      }

      if (Util.isJsonDocumentMethod(op.method)) {
        op.body match {
          case None => {
            sb.append("        HttpClient::Preconditions.assert_class('hash', hash, Hash)")
            requestBuilder.append(".with_json(hash.to_json)")
          }
          case Some(body) => {
            val ti = parseTypeInstance(body)
            sb.append("        " + ti.assertMethod)

            body match {
              case TypeInstance(_, Type.Primitive(pt)) => {
                requestBuilder.append(".with_body(${ti.varName})")
              }

              case TypeInstance(TypeContainer.Singleton, Type.Model(name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.to_hash.to_json)")
              }
              case TypeInstance(TypeContainer.List, Type.Model(name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.map { |o| o.to_hash.to_json })")
              }
              case TypeInstance(TypeContainer.Map, Type.Model(name)) => {
                sys.error("Ruby client does not yet serialize maps to json")
              }

              case TypeInstance(TypeContainer.Singleton, Type.Enum(name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.to_hash.to_json)")
              }
              case TypeInstance(TypeContainer.List, Type.Enum(name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.map { |o| o.to_hash.to_json })")
              }
              case TypeInstance(TypeContainer.Map, Type.Enum(name)) => {
                sys.error("Ruby client does not yet serialize maps to json")
              }
            }
          }
          case _ => sys.error(s"Invalid body [${op.body}]")
        }
      }
      requestBuilder.append(s".${op.method.toLowerCase}")

      val responseBuilder = new StringBuilder()

      // TODO: match on all response codes
      op.responses.headOption.map { response =>
        response.`type` match {
          case TypeInstance(_, Type.Primitive(Primitives.Unit)) => {
            responseBuilder.append("\n        nil")
          }

          case TypeInstance(container, Type.Primitive(pt)) => {
            responseBuilder.append(buildResponse(container, "value"))
          }

          case TypeInstance(container, Type.Model(name)) => {
            responseBuilder.append(buildResponse(container, name))
          }

          case TypeInstance(container, Type.Enum(name)) => {
            responseBuilder.append(buildResponse(container, name))
          }
        }
      }

      sb.append(s"        ${requestBuilder.toString}${responseBuilder.toString}")
      sb.append("      end")
    }

    sb.append("")
    sb.append("    end")

    sb.mkString("\n")
  }

  def generateModel(model: Model): String = {
    val className = RubyUtil.toClassName(model.name)

    val sb = ListBuffer[String]()

    model.description.map { desc => sb.append(GeneratorUtil.formatComment(desc, 4)) }
    sb.append(s"    class $className\n")

    sb.append("      attr_reader " + model.fields.map( f => s":${f.name}" ).mkString(", "))

    sb.append("")
    sb.append("      def initialize(incoming={})")
    sb.append("        opts = HttpClient::Helper.symbolize_keys(incoming)")

    model.fields.map { field =>
      sb.append(s"        @${field.name} = ${parseArgument(field)}")
    }

    sb.append("      end\n")

    sb.append("      def to_hash")
    sb.append("        {")
    sb.append(
      model.fields.map { field =>
        val nullable = field.`type` match {
          case TypeInstance(_, Type.Primitive(pt)) => {
            s":${field.name} => ${field.name}"
          }

          case TypeInstance(TypeContainer.Singleton, Type.Model(name)) => {
            s":${field.name} => ${field.name}.to_hash"
          }
          case TypeInstance(TypeContainer.List, Type.Model(name)) => {
            s":${field.name} => ${field.name}.map(&:to_hash)"
          }
          //case TypeInstance(TypeContainer.Map, Type.Model(name)) => {
          //s":${field.name} => ${field.name}.map(&:to_hash)"
          //  TODO: Finish map
          //}

          case TypeInstance(TypeContainer.Singleton, Type.Enum(name)) => {
            s":${field.name} => ${field.name}.value"
          }
          case TypeInstance(TypeContainer.List, Type.Enum(name)) => {
            s":${field.name} => ${field.name}.map(&:value)"
          }
          //case TypeInstance(TypeContainer.Map, Type.Enum(name)) => {
          //  TODO: Finish map
          //}
        }

        val value = if (field.required) { nullable } else { s"${field.name}.nil? ? nil : ${nullable}" }

        s":${field.name} => ${value}"
      }.mkString("            ", ",\n            ", "")
    )
    sb.append("        }")
    sb.append("      end\n")

    sb.append("    end")

    sb.mkString("\n")
  }

  private def parseArgument(field: Field): String = {
    field.`type` match {
      case TypeInstance(container, Type.Primitive(pt)) => {
        parsePrimitiveArgument(field.name, container, pt, field.required, field.default)
      }
      case TypeInstance(container, Type.Model(name)) => {
        parseModelArgument(field.name, container, name, field.required)
      }
      case TypeInstance(container, Type.Enum(name)) => {
        parseEnumArgument(field.name, container, name, field.required, field.default)
      }
    }
  }

  private def parseArgument(param: Parameter): String = {
    param.`type` match {
      case TypeInstance(container, Type.Primitive(pt)) => {
        parsePrimitiveArgument(param.name, container, pt, param.required, param.default)
      }
      case TypeInstance(container, Type.Model(name)) => {
        parseModelArgument(param.name, container, name, param.required)
      }
      case TypeInstance(container, Type.Enum(name)) => {
        parseEnumArgument(param.name, container, name, param.required, param.default)
      }
    }
  }

  private def qualifiedClassName(
    name: String
  ): String = {
    "%s::Models::%s".format(
      moduleName,
      RubyUtil.toClassName(name)
    )
  }

  private def parseModelArgument(name: String, container: TypeContainer, modelName: String, required: Boolean): String = {
    val value = s"opts.delete(:$name)"
    val klass = qualifiedClassName(modelName)

    val methodName = container match {
      case TypeContainer.Singleton => "to_instance_singleton"
      case TypeContainer.List => "to_instance_list"
      case TypeContainer.Map => "to_instance_map"
    }

    s"HttpClient::Helper.$methodName('${name}', ${klass}, ${value}, :required => $required)"
  }

  private def parseEnumArgument(name: String, container: TypeContainer, enumName: String, required: Boolean, default: Option[String]): String = {
    val value = default match {
      case None => s"opts.delete(:${name})"
      case Some(defaultValue) => s"opts.delete(:${name}) || \'$defaultValue\'"
    }

    val klass = qualifiedClassName(enumName)

    val methodName = container match {
      case TypeContainer.Singleton => "to_klass_singleton"
      case TypeContainer.List => "to_klass_list"
      case TypeContainer.Map => "to_klass_map"
    }

    s"HttpClient::Helper.$methodName('$name', $klass.apply($value), $klass, :required => $required)"
  }

  private def parsePrimitiveArgument(name: String, container: TypeContainer, pt: Primitives, required: Boolean, default: Option[String]): String = {
    val value = default match {
      case None => s"opts.delete(:${name})"
      case Some(defaultValue) => {
        pt match {
          case Primitives.String | Primitives.DateIso8601 | Primitives.DateTimeIso8601 | Primitives.Uuid => {
            s"opts.delete(:${name}) || \'$defaultValue\'"
          }
          case Primitives.Boolean => {
            s"opts.has_key?(:${name}) ? (opts.delete(:${name}) ? true : false) : $defaultValue"
          }
          case Primitives.Decimal | Primitives.Integer | Primitives.Double | Primitives.Long => {
            s"opts.delete(:${name}) || ${default.get}"
          }
          case Primitives.Unit => {
            sys.error("Cannot have a unit argument")
          }
        }
      }
    }

    val multiple = container match {
      case TypeContainer.Singleton => false
      case TypeContainer.List => true
      case TypeContainer.Map => {
        sys.error("TODO: Finish map")
      }
    }

    pt match {
      case Primitives.Decimal => {
        s"HttpClient::Helper.to_big_decimal('$name', $value, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.Uuid => {
        s"HttpClient::Helper.to_uuid('$name', $value, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.DateIso8601 => {
        s"HttpClient::Helper.to_date_iso8601('$name', $value, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.DateTimeIso8601 => {
        s"HttpClient::Helper.to_date_time_iso8601('$name', $value, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.Boolean => {
        s"HttpClient::Helper.to_boolean('$name', $value, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.Double | Primitives.Integer | Primitives.Long | Primitives.String => {
        val klass = rubyClass(pt)
        s"HttpClient::Helper.to_klass('$name', $value, $klass, :required => ${required}, :multiple => ${multiple})"
      }

      case Primitives.Unit => {
        sys.error("Cannot have a unit type as a parameter")
      }

    }

  }

  private def rubyClass(pt: Primitives): String = {
    pt match {
      case Primitives.String => "String"
      case Primitives.Long => "Integer"
      case Primitives.Double => "Float"
      case Primitives.Integer => "Integer"
      case Primitives.Boolean => "String"
      case Primitives.Decimal => "BigDecimal"
      case Primitives.Uuid => "String"
      case Primitives.DateIso8601 => "Date"
      case Primitives.DateTimeIso8601 => "DateTime"
      case Primitives.Unit => "nil"
    }
  }

  private def asString(varName: String, pt: Primitives): String = pt match {
    case Primitives.String | Primitives.Integer | Primitives.Double | Primitives.Long | Primitives.Boolean | Primitives.Decimal | Primitives.Uuid => varName
    case Primitives.DateIso8601 => s"$varName.strftime('%Y-%m-%d')"
    case Primitives.DateTimeIso8601 => s"$varName.strftime('%Y-%m-%dT%H:%M:%S%z')"
    case Primitives.Unit => {
      sys.error(s"Unsupported type[$pt] for string formatting - varName[$varName]")
    }
  }

  case class RubyTypeInfo(
    varName: String,
    klass: String,
    assertMethod: String
  )

  private def parseTypeInstance(
    instance: TypeInstance
  ): RubyTypeInfo = {
    val klass = instance.`type` match {
      case Type.Primitive(pt) => rubyClass(pt)
      case Type.Model(name) => qualifiedClassName(name)
      case Type.Enum(name) => qualifiedClassName(name)
    }

    val varName = instance match {
      case TypeInstance(TypeContainer.Singleton, Type.Primitive(pt)) => RubyUtil.toVariable("value", multiple = false)
      case TypeInstance(TypeContainer.List, Type.Primitive(pt)) => RubyUtil.toVariable("value", multiple = true)
      case TypeInstance(TypeContainer.Map, Type.Primitive(pt)) => RubyUtil.toVariable("value", multiple = true)

      case TypeInstance(TypeContainer.Singleton, Type.Model(name)) => RubyUtil.toVariable(name, multiple = false)
      case TypeInstance(TypeContainer.List, Type.Model(name)) => RubyUtil.toVariable(name, multiple = true)
      case TypeInstance(TypeContainer.Map, Type.Model(name)) => RubyUtil.toVariable(name, multiple = true)

      case TypeInstance(TypeContainer.Singleton, Type.Enum(name)) => RubyUtil.toVariable(name, multiple = false)
      case TypeInstance(TypeContainer.List, Type.Enum(name)) => RubyUtil.toVariable(name, multiple = true)
      case TypeInstance(TypeContainer.Map, Type.Enum(name)) => RubyUtil.toVariable(name, multiple = true)
    }

    val assertStub = instance.container match {
      case TypeContainer.Singleton => "assert_class"
      case TypeContainer.List => "assert_collection_of_class"
      case TypeContainer.Map => "assert_hash_of_class"
    }

    RubyTypeInfo(
      varName = varName,
      klass = klass,
      assertMethod = s"HttpClient::Preconditions.$assertStub('$varName', $varName, $klass)"
    )
  }

  private def buildResponse(
    container: TypeContainer,
    name: String
  ): String = {
    val varName = qualifiedClassName(name)
    val code = s" { |hash| $varName.new(hash) }"
    container match {
      case TypeContainer.Singleton => code
      case TypeContainer.List => ".map" + code
      // TODO: Finish map
    }
  }

}
