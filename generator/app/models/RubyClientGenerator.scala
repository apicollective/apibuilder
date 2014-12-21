package models

import com.gilt.apidocspec.models._
import core._
import lib.{Methods, Primitives}
import lib.Text._
import generator.{GeneratorUtil, CodeGenerator, ScalaUtil}
import scala.collection.mutable.ListBuffer

object RubyUtil {

  def toClassName(
    name: String,
    multiple: Boolean = false
  ): String = {
    ScalaUtil.toClassName(name, multiple)
  }

  def toDefaultVariable(
    multiple: Boolean = false
  ): String = {
    toVariable("value", multiple = multiple)
  }

  def toVariable(
    name: String,
    multiple: Boolean = false
  ): String = {
    val value = lib.Text.initLowerCase(lib.Text.camelCaseToUnderscore(name).toLowerCase)
    multiple match {
      case true => lib.Text.pluralize(value)
      case false => value
    }
  }

  def wrapInQuotes(value: String): String = {
    if (value.indexOf("'") < 0) {
      s"'$value'"
    } else if (value.indexOf("\"") < 0) {
      s""""$value""""
    } else {
      sys.error("TODO: Support quoting quotes")
    }
  }

}

object RubyClientGenerator extends CodeGenerator {

  override def generate(sd: Service): String = {
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
      lines.append(s"  end")
      lines.append("")
    }

    lines.append("end")
    lines.mkString("\n")
  }

  def enumName(value: String): String = {
    if (value == value.toUpperCase) {
     lib.Text.camelCaseToUnderscore(value.toLowerCase).split("_").map(lib.Text.initLowerCase(_)).mkString("_")
    } else {
     lib.Text.camelCaseToUnderscore(value).split("_").map(lib.Text.initLowerCase(_)).mkString("_")
    }
  }

  def apply(sd: Service, userAgent: String): RubyClientGenerator = RubyClientGenerator(sd)

}

/**
 * Generates a Ruby Client file based on the service description
 * from api.json
 */
case class RubyClientGenerator(service: Service) {
  private val moduleName = RubyUtil.toClassName(service.name)

  def generate(): String = {
    ApidocHeaders(service.userAgent).toRubyString() +
    "\n\n" +
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
    service.models.map { generateModel(_) }.mkString("\n\n").indent(4) + "\n\n" +
    "  end\n\n  # ===== END OF SERVICE DEFINITION =====\n  " +
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
            case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => {
              val code = asString(RubyUtil.toVariable(varName), name, escape = true)
              s"#{$code}"
            }
            case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => sys.error("Models cannot be in the path")
            case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => {
              val code = RubyUtil.toVariable(varName)
              s"#{$code.value}"
            }
            case TypeInstance(Container.List, _) => sys.error("Cannot have lists in the path")
            case TypeInstance(Container.Map, _) => sys.error("Cannot have maps in the path")
            case TypeInstance(Container.UNDEFINED(container), _) => sys.error(s"Cannot have container[$container] in the path")
          }
        } else {
          name
        }
      }.mkString("/")

      val methodName =lib.Text.camelCaseToUnderscore(
        GeneratorUtil.urlToMethodName(
          resource.model.plural, resource.path, op.method, op.path
        )
      ).toLowerCase

      val paramStrings = ListBuffer[String]()
      pathParams.map(_.name).foreach { n => paramStrings.append(RubyUtil.toVariable(n)) }

      if (Methods.isJsonDocumentMethod(op.method)) {
        op.body.map(_.`type`) match {
          case None => paramStrings.append("hash")

          case Some(TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name))) => paramStrings.append(RubyUtil.toDefaultVariable(multiple = false))
          case Some(TypeInstance(Container.List | Container.Map, Type(TypeKind.Primitive, name))) => paramStrings.append(RubyUtil.toDefaultVariable(multiple = true))

          case Some(TypeInstance(Container.Singleton, Type(TypeKind.Model, name))) => paramStrings.append(RubyUtil.toVariable(name, multiple = false))
          case Some(TypeInstance(Container.List | Container.Map, Type(TypeKind.Model, name))) => paramStrings.append(RubyUtil.toVariable(name, multiple = true))

          case Some(TypeInstance(Container.Singleton, Type(TypeKind.Enum, name))) => paramStrings.append(RubyUtil.toVariable(name, multiple = false))
          case Some(TypeInstance(Container.List | Container.Map, Type(TypeKind.Enum, name))) => paramStrings.append(RubyUtil.toVariable(name, multiple = true))

          case Some(TypeInstance(Container.UNDEFINED(container), _)) => sys.error(s"Unsupported container[$container]")
          case Some(TypeInstance(_, Type(kind, name))) => sys.error(s"Unsupported typeKind[$kind]")
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
        val ti = parseTypeInstance(param.`type`, fieldName = Some(param.name))
        sb.append(s"        " + ti.assertMethod)
      }

      if (!queryParams.isEmpty) {
        val paramBuilder = ListBuffer[String]()

        queryParams.foreach { param =>
          paramBuilder.append(s":${param.name} => ${parseArgument(param.name, param.`type`, param.required, param.default)}")
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

      if (Methods.isJsonDocumentMethod(op.method)) {
        op.body.map(_.`type`) match {
          case None => {
            sb.append("        HttpClient::Preconditions.assert_class('hash', hash, Hash)")
            requestBuilder.append(".with_json(hash.to_json)")
          }
          case Some(body) => {
            val ti = parseTypeInstance(body)
            sb.append("        " + ti.assertMethod)

            body match {
              case TypeInstance(_, Type(TypeKind.Primitive, name)) => {
                requestBuilder.append(s".with_body(${ti.varName})")
              }

              case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.to_json)")
              }
              case TypeInstance(Container.List, Type(TypeKind.Model, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.map { |o| o.to_hash }.to_json)")
              }
              case TypeInstance(Container.Map, Type(TypeKind.Model, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.inject({}) { |hash, o| hash[o[0]] = o[1].nil? ? nil : o[1].to_hash; hash }).to_json")
              }

              case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.to_json)")
              }
              case TypeInstance(Container.List, Type(TypeKind.Enum, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.map { |o| o.to_json })")
              }
              case TypeInstance(Container.Map, Type(TypeKind.Enum, name)) => {
                requestBuilder.append(s".with_json(${ti.varName}.inject({}) { |hash, o| hash[o[0]] = o[1].nil? ? nil : o[1].to_hash; hash }).to_json")
              }

              case TypeInstance(Container.UNDEFINED(container), _) => {
                sys.error(s"Unsupported container[$container]")
              }
              case TypeInstance(_, Type(TypeKind.UNDEFINED(kind), name)) => {
                sys.error(s"Unsupported typeKind[$kind] w/ name[$name]")
              }
            }
          }
          case _ => sys.error(s"Invalid body [${op.body}]")
        }
      }
      requestBuilder.append(s".${op.method.toLowerCase}")

      val response = generateResponses(op)

      sb.append(s"        ${requestBuilder.toString}$response")
      sb.append("      end")
    }

    sb.append("")
    sb.append("    end")

    sb.mkString("\n")
  }

  def generateModel(model: Model): String = {
    val className = RubyUtil.toClassName(model.name)

    val sb = ListBuffer[String]()

    model.description.map { desc => sb.append(GeneratorUtil.formatComment(desc)) }
    sb.append(s"class $className\n")

    sb.append("  attr_reader " + model.fields.map( f => s":${f.name}" ).mkString(", "))

    sb.append("")
    sb.append("  def initialize(incoming={})")
    sb.append("    opts = HttpClient::Helper.symbolize_keys(incoming)")

    model.fields.map { field =>
      sb.append(s"    @${field.name} = ${parseArgument(field.name, field.`type`, field.required, field.default)}")
    }

    sb.append("  end\n")

    sb.append("  def to_json")
    sb.append("    JSON.dump(to_hash)")
    sb.append("  end\n")

    sb.append("  def copy(incoming={})")
    sb.append(s"    $className.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))")
    sb.append("  end\n")

    sb.append("  def to_hash")
    sb.append("    {")
    sb.append(
      model.fields.map { field =>
        val value = field.`type` match {
          case TypeInstance(_, Type(TypeKind.Primitive, name)) => {
            Primitives(name) match {
              case Some(Primitives.Object) | Some(Primitives.Unit) => field.name
              case _ => asString(field.name, name, escape = false)
            }
          }

          case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => {
            s"${field.name}.nil? ? nil : ${field.name}.to_hash"
          }
          case TypeInstance(Container.List, Type(TypeKind.Model, name)) => {
            s"(${field.name} || []).map(&:to_hash)"
          }
          case TypeInstance(Container.Map, Type(TypeKind.Model, name)) => {
            s"(${field.name} || {}).inject({}).map { |h, o| h[o[0]] = o[1].nil? ? nil : o[1].to_hash; h }"
          }

          case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => {
            s"${field.name}.nil? ? nil : ${field.name}.value"
          }
          case TypeInstance(Container.List, Type(TypeKind.Enum, name)) => {
            s"(${field.name} || []).map(&:value)"
          }
          case TypeInstance(Container.Map, Type(TypeKind.Enum, name)) => {
            s"(${field.name} || {}).inject({}).map { |h, o| h[o[0]] = o[1].nil? ? nil : o[1].value; h }"
          }
        }

        s":${field.name} => ${value}"
      }.mkString(",\n").indent(6)
    )
    sb.append("    }")
    sb.append("  end\n")

    sb.append("end")

    sb.mkString("\n")
  }

  private def withHelper(
    method: String,
    code: String
  ) = s"$method($code)"

  private def parseArgumentPrimitive(
    fieldName: String,
    value: String,
    ptName: String,
    required: Boolean,
    default: Option[String]
  ): String = {
    val pt = Primitives(ptName).getOrElse {
      sys.error("Invalid primitive[$ptName]")
    }
    val arg = pt match {
      case Primitives.String | Primitives.Integer | Primitives.Double | Primitives.Long | Primitives.Boolean => {
        value
      }

      case Primitives.DateIso8601 => {
        withHelper("HttpClient::Helper.to_date_iso8601", value)
      }

      case Primitives.DateTimeIso8601 => {
        withHelper("HttpClient::Helper.to_date_time_iso8601", value)
      }

      case Primitives.Uuid => {
        withHelper("HttpClient::Helper.to_uuid", value)
      }

      case Primitives.Decimal => {
        withHelper("HttpClient::Helper.to_big_decimal", value)
      }

      case Primitives.Object => {
        withHelper("HttpClient::Helper.to_object", value)
      }

      case Primitives.Unit => {
        sys.error("Cannot have a unit parameter")
      }
    }

    val mustBeSpecified = required && default.isEmpty

    (pt, mustBeSpecified) match {
      case (Primitives.Boolean, true) => {
        s"HttpClient::Preconditions.assert_boolean('$fieldName', $arg)"
      }
      case (Primitives.Boolean, false) => {
        s"HttpClient::Preconditions.assert_boolean_or_nil('$fieldName', $arg)"
      }
      case (_, req) => {
        val className = rubyClass(pt)
        val assertMethod = if (req) { "assert_class" } else { "assert_class_or_nil" }
        s"HttpClient::Preconditions.$assertMethod('$fieldName', $arg, $className)"
      }
    }
  }

  private def withDefaultArray(
    fieldName: String,
    arg: String,
    required: Boolean
  ) = required match {
    case false => s"($arg || [])"
    case true => s"HttpClient::Preconditions.assert_class('$fieldName', $arg, Array)"
  }

  private def withDefaultMap(
    fieldName: String,
    arg: String,
    required: Boolean
  ) = required match {
    case false => s"($arg || {})"
    case true => s"HttpClient::Preconditions.assert_class('$fieldName', $arg, Hash)"
  }

  private def parseArgument(
    fieldName: String,
    ti: TypeInstance,
    required: Boolean,
    default: Option[String]
  ): String = {
    ti match {
      case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => {
        parseArgumentPrimitive(fieldName, s"opts.delete(:$fieldName)", name, required, default)
      }

      case TypeInstance(Container.List, Type(TypeKind.Primitive, name)) => {
        withDefaultArray(fieldName, s"opts.delete(:$fieldName)", required) + ".map { |v| " + parseArgumentPrimitive(fieldName, "v", name, required, default) + "}"
      }

      case TypeInstance(Container.Map, Type(TypeKind.Primitive, name)) => {
        withDefaultMap(fieldName, s"opts.delete(:$fieldName)", required) + ".inject({}) { |h, d| h[d[0]] = " + parseArgumentPrimitive(fieldName, "d[1]", name, required, default) + "; h }"
      }

      case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => {
        val klass = qualifiedClassName(name)
        wrapWithAssertion(
          fieldName,
          klass,
          required,
          s"opts[:$fieldName].nil? ? nil : (opts[:$fieldName].is_a?($klass) ? opts.delete(:$fieldName) : $klass.new(opts.delete(:$fieldName)))"
        )
      }

      case TypeInstance(Container.List, Type(TypeKind.Model, name)) => {
        val klass = qualifiedClassName(name)
        withDefaultArray(fieldName, s"opts.delete(:$fieldName)", required) + ".map { |el| " + s"el.nil? ? nil : (el.is_a?($klass) ? el : $klass.new(el)) }"
      }

      case TypeInstance(Container.Map, Type(TypeKind.Model, name)) => {
        val klass = qualifiedClassName(name)
        withDefaultMap(fieldName, s"opts.delete(:$fieldName)", required) + ".inject({}) { |h, el| h[el[0]] = " + s"el[1].nil? ? nil : (el[1].is_a?($klass) ? el[1] : $klass.new(el[1])); h" + "}"
      }

      case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => {
        val klass = qualifiedClassName(name)
        wrapWithAssertion(
          fieldName,
          klass,
          required,
          s"opts[:$fieldName].nil? ? nil : (opts[:$fieldName].is_a?($klass) ? opts.delete(:$fieldName) : $klass.apply(opts.delete(:$fieldName)))"
        )
      }

      case TypeInstance(Container.List, Type(TypeKind.Enum, name)) => {
        val klass = qualifiedClassName(name)
        withDefaultArray(fieldName, s"opts.delete(:$fieldName)", required) + s".map { |el| el.nil? ? nil : (el.is_a?($klass) ? el : $klass.apply(el)) }"
      }

      case TypeInstance(Container.Map, Type(TypeKind.Enum, name)) => {
        val klass = qualifiedClassName(name)
        withDefaultMap(fieldName, s"opts.delete(:$fieldName)", required) + s".inject({}) { |h, el| h[el[0]] = el[1].nil? ? nil : (el[1].is_a?($klass) ? el[1] : $klass.apply(el[1]); h }"
      }
    }
  }

  private def wrapWithAssertion(
    fieldName: String,
    className: String,
    required: Boolean,
    code: String
  ): String = {
    val assertMethod = if (required) { "assert_class" } else { "assert_class_or_nil" }
    s"HttpClient::Preconditions.$assertMethod('$fieldName', $code, $className)"
  }

  private def qualifiedClassName(
    name: String
  ): String = {
    "%s::Models::%s".format(
      moduleName,
      RubyUtil.toClassName(name)
    )
  }

  private def rubyClass(pt: Primitives): String = {
    pt match {
      case Primitives.Boolean => "String"
      case Primitives.Decimal => "BigDecimal"
      case Primitives.Double => "Float"
      case Primitives.Integer => "Integer"
      case Primitives.Long => "Integer"
      case Primitives.DateIso8601 => "Date"
      case Primitives.DateTimeIso8601 => "DateTime"
      case Primitives.Object => "Hash"
      case Primitives.String => "String"
      case Primitives.Unit => "nil"
      case Primitives.Uuid => "String"
    }
  }

  private def asString(
    varName: String,
    ptName: String,
    escape: Boolean
  ): String = {
    Primitives(ptName).getOrElse {
      sys.error(s"Unknown primitive type[$ptName]")
    } match {
      case Primitives.Integer | Primitives.Double | Primitives.Long | Primitives.Uuid | Primitives.Decimal | Primitives.Boolean => varName
      case Primitives.String => {
        if (escape) {
          s"CGI.escape($varName)"
        } else {
          varName
        }
      }
      case Primitives.DateIso8601 => s"HttpClient::Helper.date_iso8601_to_string($varName)"
      case Primitives.DateTimeIso8601 => s"HttpClient::Helper.date_time_iso8601_to_string($varName)"
      case Primitives.Object | Primitives.Unit => {
        sys.error(s"Unsupported type[$ptName] for string formatting - varName[$varName]")
      }
    }
  }

  case class RubyTypeInfo(
    varName: String,
    klass: String,
    assertMethod: String
  )

  private def parseTypeInstance(
    instance: TypeInstance,
    fieldName: Option[String] = None
  ): RubyTypeInfo = {
    val klass = instance.`type` match {
      case Type(TypeKind.Primitive, ptName) => Primitives(ptName) match {
        case None => sys.error(s"Unsupported primitive[$ptName] for instance[$instance]")
        case Some(pt) => rubyClass(pt)
      }
      case Type(TypeKind.Model, name) => qualifiedClassName(name)
      case Type(TypeKind.Enum, name) => qualifiedClassName(name)
      case Type(TypeKind.UNDEFINED(kind), name) => sys.error(s"Unsupported typeKind[$kind] for var[$name]")
    }

    val varName = instance match {
      case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => {
        fieldName match {
          case Some(n) => RubyUtil.toVariable(n, multiple = false)
          case None => RubyUtil.toDefaultVariable(multiple = false)
        }
      }
      case TypeInstance(Container.List | Container.Map, Type(TypeKind.Primitive, name)) => {
        fieldName match {
          case Some(name) => RubyUtil.toVariable(name, multiple = true)
          case None => RubyUtil.toDefaultVariable(multiple = true)
        }
      }

      case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => RubyUtil.toVariable(fieldName.getOrElse(name), multiple = false)
      case TypeInstance(Container.List | Container.Map, Type(TypeKind.Model, name)) => RubyUtil.toVariable(fieldName.getOrElse(name), multiple = true)

      case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => RubyUtil.toVariable(fieldName.getOrElse(name), multiple = false)
      case TypeInstance(Container.List | Container.Map, Type(TypeKind.Enum, name)) => RubyUtil.toVariable(fieldName.getOrElse(name), multiple = true)
    }

    val assertStub = instance.container match {
      case Container.Singleton => "assert_class"
      case Container.Option => "assert_class_or_nil"
      case Container.List => "assert_collection_of_class"
      case Container.Map => "assert_hash_of_class"
      case Container.Union => {
        sys.error("TODO: union type not yet supported in ruby clients")
      }
      case Container.UNDEFINED(container) => {
        sys.error(s"Invalid container[$container]")
      }
    }

    RubyTypeInfo(
      varName = varName,
      klass = klass,
      assertMethod = s"HttpClient::Preconditions.$assertStub('$varName', $varName, $klass)"
    )
  }

  def generateResponses(op: Operation): String = {
    // TODO: match on all response codes
    op.responses.headOption.map { response =>
      generateResponse(response) match {
        case None => "\n        nil"
        case Some(v) => v
      }
    }.mkString("\n")
  }

  def generateResponse(response: Response): Option[String] = {
    response.`type` match {
      case TypeInstance(container, Type(TypeKind.Primitive, name)) => {
        Primitives(name).getOrElse {
          sys.error(s"Unknown primitive type[$name]")
        } match {
          case Primitives.Unit => None
          case pt => {
            Some(buildResponse(container, RubyUtil.toDefaultVariable()))
          }
        }
      }

      case TypeInstance(container, Type(TypeKind.Model, name)) => {
        Some(buildResponse(container, name))
      }

      case TypeInstance(container, Type(TypeKind.Enum, name)) => {
        Some(buildResponse(container, name))
      }

      case TypeInstance(Container.UNDEFINED(container), _) => {
        sys.error(s"Invalid container[$container]")
      }

      case TypeInstance(_, Type(TypeKind.UNDEFINED(kind), name)) => {
        sys.error(s"Unsupported typeKind[$kind] w/ name[$name]")
      }
    }
  }

  private def buildResponse(
    container: Container,
    name: String
  ): String = {
    val varName = qualifiedClassName(name)
    val mapSingleObject = s" { |hash| $varName.new(hash) }"
    container match {
      case Container.Singleton | Container.Option => {
        mapSingleObject
      }
      case Container.List => {
        ".map" + mapSingleObject
      }
      case Container.Map => {
        s".inject({}) { |hash, o| hash[o[0]] = o[1].nil? ? nil : $varName.new(hash); hash }"
      }
      case Container.Union => {
        sys.error("TODO: union type")
      }
      case Container.UNDEFINED(container) => {
        sys.error(s"Invalid container[$container]")
      }
    }
  }

}
