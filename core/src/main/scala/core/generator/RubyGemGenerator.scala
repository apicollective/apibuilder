package core.generator

import core._
import scala.collection.mutable.ListBuffer

object RubyGemGenerator {
  def apply(json: String) = {
    new RubyGemGenerator(ServiceDescription(json)).generate
  }
}

/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class RubyGemGenerator(service: ServiceDescription) {

  private val moduleName = Text.safeName(service.name)

  private val Underscores = """([\_+])""".r
  private val moduleKey = {
    val name = Text.camelCaseToUnderscore(moduleName).toLowerCase
    Underscores.replaceAllIn(name, m => s"-")
  }

  def generate(): String = {
    RubyHttpClient.require +
    "\n" +
    service.description.map { desc => GeneratorUtil.formatComment(desc) + "\n" }.getOrElse("") +
    s"module ${moduleName}\n" +
    generateClient() +
    "\n\n  module Models\n" +
    service.models.map { generateModel(_) }.mkString("\n\n") +
    "\n\n  end" +
    "\n\n  module Clients\n" +
    service.resources.map { res => generateClientForResource(res) }.mkString("\n\n") +
    "\n\n  end\n\n" +
    RubyHttpClient.contents +
    "end"
  }

  private def generateClient(): String = {
    val sb = ListBuffer[String]()
    val url = service.baseUrl

    sb.append(s"""
  class Client

    def initialize(url, opts={})
      HttpClient::Preconditions.assert_class('url', url, String)
      @url = URI.parse(url)
      @authorization = HttpClient::Preconditions.assert_class_or_nil('authorization', opts.delete(:authorization), HttpClient::Authorization)
      HttpClient::Preconditions.assert_empty_opts(opts)
      HttpClient::Preconditions.check_state(url.match(/http.+/i), "URL[%s] must start with http" % url)
    end

    def Client.authorize(url, opts={})
      HttpClient::Preconditions.assert_class('url', url, String)
      token = HttpClient::Preconditions.assert_class_or_nil('token', opts.delete(:token), String)
      HttpClient::Preconditions.assert_empty_opts(opts)

      if token
        Client.new(url, :authorization => HttpClient::Authorization.basic(token))
      else
        Client.new(url)
      end
    end

    def request(path=nil)
      HttpClient::Preconditions.assert_class_or_nil('path', path, String)
      request = HttpClient::Request.new(@url + path.to_s)

      if @authorization
        request.with_auth(@authorization)
      else
        request
      end
    end
""")

    sb.append(service.models.map { model =>
      val className = Text.underscoreToInitCap(model.plural)

      s"    def ${model.plural}\n" +
      s"      @${model.plural} ||= ${moduleName}::Clients::${className}.new(self)\n" +
      "    end"
    }.mkString("\n\n"))

    sb.append("  end")

    sb.mkString("\n")
  }

  def generateClientForResource(resource: Resource): String = {
    val className = Text.underscoreToInitCap(resource.model.plural)

    val sb = ListBuffer[String]()
    sb.append(s"    class ${className}")
    sb.append("")
    sb.append("      def initialize(client)")
    sb.append(s"        @client = HttpClient::Preconditions.assert_class('client', client, ${moduleName}::Client)")
    sb.append("      end")

    resource.operations.foreach { op =>
      val pathParams = op.parameters.filter { p => p.location == ParameterLocation.Path }
      val otherParams = op.parameters.filter { p => p.location != ParameterLocation.Path }

      val rubyPath = op.path.split("/").map { name =>
        if (name.startsWith(":")) {
          "#{" + name.slice(1, name.length) + "}"
        } else {
          name
        }
      }.mkString("/")

      val methodName = {
        Text.camelCaseToUnderscore(GeneratorUtil.urlToMethodName(resource.path, op.method, op.path)).toLowerCase
      }

      val paramStrings = ListBuffer[String]()
      pathParams.map(_.name).foreach { n => paramStrings.append(n) }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        paramStrings.append("body")
      }

      paramStrings.append("incoming={}")

      sb.append("")
      op.description.map { desc =>
        sb.append(GeneratorUtil.formatComment(desc, 6))
      }
      sb.append(s"      def ${methodName}(" + paramStrings.mkString(", ") + ")")

      pathParams.foreach { param =>

        val klass = param.paramtype match {
          case t: PrimitiveParameterType => rubyClass(t.datatype)
          case m: ModelParameterType => Text.underscoreToInitCap(m.model.name)
        }

        sb.append(s"        HttpClient::Preconditions.assert_class('${param.name}', ${param.name}, ${klass})")
      }

      val paramBuilder = ListBuffer[String]()

      otherParams.foreach { param =>
        paramBuilder.append(s":${param.name} => ${parseArgument(param)}")
      }

      sb.append("        opts = HttpClient::Helper.symbolize_keys(incoming)")
      sb.append("        query = {")
      sb.append("          " + paramBuilder.mkString(",\n          "))
      sb.append("        }.delete_if { |k, v| v.nil? }")

      val requestBuilder = new StringBuilder()
      requestBuilder.append("@client.request(\"" + rubyPath + "\")")

      requestBuilder.append(".with_query(query)")

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        val body = op.body.get
        val modelName = Text.underscoreToInitCap(body.model.name)
        val fullModelName = s"$moduleName::Models::$modelName"
        if (body.multiple) {
          sb.append(s"        HttpClient::Preconditions.assert_collection_of_class('body', body, $fullModelName)")
        } else {
          sb.append(s"        HttpClient::Preconditions.assert_class('body', body, $fullModelName)")
        }
        requestBuilder.append(".with_json(body.to_json)")
      }
      requestBuilder.append(s".${op.method.toLowerCase}")

      val responseBuilder = new StringBuilder()

      // TODO: match on all response codes
      op.responses.headOption.map { response =>
        response.datatype match {
          case Datatype.UnitType.name => {
            responseBuilder.append("\n        nil")
          }

          case resourceName: String => {
            if (op.responses.head.multiple) {
              responseBuilder.append(".map")
            }
            responseBuilder.append(s" { |hash| ${moduleName}::Models::${Text.underscoreToInitCap(resourceName)}.new(hash) }")
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

  def generateModel(model: core.Model): String = {
    val className = Text.underscoreToInitCap(model.name)

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

    model.fields.foreach { f =>
      f.fieldtype match {
        case PrimitiveFieldType(Datatype.BooleanType) => {
          sb.append(s"""
      def ${f.name}?
        ${f.name}
      end
""")
        }
        case _ =>
      }
    }

    sb.append("""
      def ==(obj)
""" + model.fields.map { f =>
  val name = f.name
s"""           $name == obj.$name"""
}.mkString(" &&\n") + """
      end
""")

    sb.append("""
      def to_json
        {""" + model.fields.map { f =>
          if (f.isPrimitive) {
s"""          "${f.name}" => ${f.name}"""
          } else {
s"""          "${f.name}" => ${f.name}.to_json"""
          }
        }.mkString("\n", ",\n", "") + """
        }.to_json
      end
""")

    sb.append("    end")

    sb.mkString("\n")
  }

  private def parseArgument(field: Field): String = {
    field.fieldtype match {
      case PrimitiveFieldType(datatype: Datatype) => {
        parsePrimitiveArgument(field.name, datatype, field.required, field.default, field.multiple)
      }
      case ModelFieldType(model: Model) => {
        parseModelArgument(field.name, model, field.required, field.multiple)
      }
      case ReferenceFieldType(referencedModelName: String) => {
        parseReferenceArgument(field.name, referencedModelName, field.required)
      }
    }

  }

  private def parseArgument(param: Parameter): String = {
    param.paramtype match {
      case dt: PrimitiveParameterType => {
        parsePrimitiveArgument(param.name, dt.datatype, param.required, param.default, param.multiple)
      }
      case mt: ModelParameterType => {
        parseModelArgument(param.name, mt.model, param.required, param.multiple)
      }
    }
  }

  // TODO: Validate how we want references to work. Maybe validate
  // that the reference has the right model
  private def parseReferenceArgument(name: String, referencedModelName: String, required: Boolean): String = {
    val value = s"opts.delete(:${name})"
    val assertMethod = if (required) { "assert_class" } else { "assert_class_or_nil" }
    s"HttpClient::Preconditions.${assertMethod}('${name}', ${value}, Reference)"
  }

  private def parseModelArgument(name: String, model: Model, required: Boolean, multiple: Boolean): String = {
    val value = s"opts.delete(:${name})"
    val klass = Text.underscoreToInitCap(model.name)
    s"HttpClient::Helper.to_model_instance('${name}', ${klass}, ${value}, :required => $required, :multiple => $multiple)"
  }

  private def parsePrimitiveArgument(name: String, datatype: Datatype, required: Boolean, default: Option[String], multiple: Boolean): String = {
    val value = if (default.isEmpty) {
      s"opts.delete(:${name})"
    } else if (datatype == Datatype.StringType) {
      s"opts.delete(:${name}) || \'${default.get}\'"
    } else if (datatype == Datatype.BooleanType) {
      s"opts.has_key?(:${name}) ? (opts.delete(:${name}) ? true : false) : ${default.get}"
    } else {
      s"opts.delete(:${name}) || ${default.get}"
    }

    val hasValue = (required || !default.isEmpty)
    val assertMethod = if (hasValue) { "assert_class" } else { "assert_class_or_nil" }
    val klass = rubyClass(datatype)

    if (datatype == Datatype.DecimalType) {
      s"HttpClient::Helper.to_big_decimal('$name', $value, :required => ${required}, :multiple => ${multiple})"

    } else if (datatype == Datatype.UuidType) {
      s"HttpClient::Helper.to_uuid('$name', $value, :required => ${required}, :multiple => ${multiple})"

    } else if (datatype == Datatype.DateTimeIso8601Type) {
      s"HttpClient::Helper.to_date_time_iso8601('$name', $value, :required => ${required}, :multiple => ${multiple})"

    } else if (datatype == Datatype.MoneyIso4217Type) {
      s"HttpClient::Helper.to_money_iso4217('$name', $value, :required => ${required}, :multiple => ${multiple})"

    } else if (datatype == Datatype.BooleanType) {
      s"HttpClient::Helper.to_boolean('$name', $value, :required => ${required}, :multiple => ${multiple})"

    } else {
      s"HttpClient::Helper.to_klass('$name', $value, ${klass}, :required => ${required}, :multiple => ${multiple})"
    }

  }

  private def rubyClass(datatype: Datatype): String = {
    datatype match {
      case Datatype.StringType => "String"
      case Datatype.LongType => "Integer"
      case Datatype.DoubleType => "Float"
      case Datatype.IntegerType => "Integer"
      case Datatype.BooleanType => "String"
      case Datatype.DecimalType => "BigDecimal"
      case Datatype.UuidType => "String"
      case Datatype.DateTimeIso8601Type => "DateTime"
      case Datatype.MoneyIso4217Type => "HttpClient::Types::MoneyIso4217Type"
      case Datatype.UnitType => "nil"
      case _ => {
        sys.error(s"Cannot map data type[${datatype}] to ruby class")
      }
    }
  }


}
