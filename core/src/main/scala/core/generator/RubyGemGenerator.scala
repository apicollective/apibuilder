package core.generator

import core.{ Datatype, Field, ServiceDescription, Model, Operation, Text }

import scala.collection.mutable.ListBuffer

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
    val operationNames = service.operations.map { _.model.name }.distinct.sorted

    RubyHttpClient.require +
    "\n" +
    service.description.map { desc => GeneratorUtil.formatComment(desc) + "\n" }.getOrElse("") +
    s"module ${moduleName}\n" +
    generateClient() +
    "\n\n  module Models\n" +
    service.models.map { generateModel(_) }.mkString("\n\n") +
    "\n\n  end" +
    "\n\n  module Clients\n" +
    operationNames.map { name => generateClientForResource(name, service.operations.filter { _.model.name == name}) }.mkString("\n\n") +
    "\n\n  end\n\n" +
    RubyHttpClient.contents +
    "end"
  }

  private def generateClient(): String = {
    val sb = ListBuffer[String]()
    val url = service.baseUrl + service.basePath.getOrElse("")

    sb.append(s"""
  class Client

    def initialize(url, opts={})
      HttpClient::Preconditions.assert_class(url, String)
      @url = URI.parse(url)
      @authorization = HttpClient::Preconditions.assert_class_or_nil(opts.delete(:authorization), HttpClient::Authorization)
      HttpClient::Preconditions.assert_empty_opts(opts)
      HttpClient::Preconditions.check_state(url.match(/http.+/i), "URL[%s] must start with http" % url)
    end

    def Client.authorize(url, opts={})
      HttpClient::Preconditions.assert_class(url, String)
      token = HttpClient::Preconditions.assert_class_or_nil(opts.delete(:token), String)
      HttpClient::Preconditions.assert_empty_opts(opts)

      if token
        Client.new(url, :authorization => HttpClient::Authorization.basic(token))
      else
        Client.new(url)
      end
    end

    def request(path=nil)
      HttpClient::Preconditions.assert_class_or_nil(path, String)
      request = HttpClient::Request.new(@url + path.to_s)

      if @authorization
        request.with_auth(@authorization)
      else
        request
      end
    end
""")

    sb.append(service.models.map { model =>
      val className = modelClassName(model.name)

      s"    def ${model.name}\n" +
      s"      @${model.name} ||= ${moduleName}::Clients::${className}.new(self)\n" +
      "    end"
    }.mkString("\n\n"))

    sb.append("  end")

    sb.mkString("\n")
  }

  def generateClientForResource(resourceName: String, operations: Seq[Operation]): String = {
    require(!operations.isEmpty)

    val resourcePath = s"/${resourceName}"
    val className = Text.underscoreToInitCap(resourceName)

    val sb = ListBuffer[String]()
    sb.append(s"    class ${className}")
    sb.append("")
    sb.append("      def initialize(client)")
    sb.append(s"        @client = HttpClient::Preconditions.assert_class(client, ${moduleName}::Client)")
    sb.append("      end")

    operations.foreach { op =>
      val path = op.path.getOrElse(resourcePath)

      val namedParams = GeneratorUtil.namedParametersInPath(path)
      val pathParams = op.parameters.filter { p => namedParams.contains(p.name) }
      val otherParams = op.parameters.filter { p => !namedParams.contains(p.name) }

      val rubyPath = path.split("/").map { name =>
        if (name.startsWith(":")) {
          "#{" + name.slice(1, name.length) + "}"
        } else {
          name
        }
      }.mkString("/")

      val methodName = op.method.toLowerCase + op.path.getOrElse("").split("/").map { name =>
        if (name.startsWith(":")) {
          name.slice(1, name.length)
        } else {
          name
        }
      }.mkString("_")

      val paramStrings = ListBuffer[String]()
      pathParams.map(_.name).foreach { n => paramStrings.append(n) }

      val hasQueryParams = (!GeneratorUtil.isJsonDocumentMethod(op.method) && !otherParams.isEmpty)
      if (hasQueryParams) {
        paramStrings.append("incoming={}")
      }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        paramStrings.append("hash")
      }

      sb.append("")
      op.description.map { desc =>
        sb.append(GeneratorUtil.formatComment(desc, 6))
      }
      sb.append(s"      def ${methodName}(" + paramStrings.mkString(", ") + ")")

      pathParams.foreach { param =>
        val klass = rubyClass(param.datatype)
        sb.append(s"        HttpClient::Preconditions.assert_class(${param.name}, ${klass})")
      }

      if (hasQueryParams) {
        val paramBuilder = ListBuffer[String]()

        otherParams.foreach { param =>
          paramBuilder.append(s":${param.name} => ${parseArgument(param)}")
        }

        sb.append("        opts = HttpClient::Helper.symbolize_keys(incoming)")
        sb.append("        query = {")
        sb.append("          " + paramBuilder.mkString(",\n          "))
        sb.append("        }.delete_if { |k, v| v.nil? }")
        sb.append(s"        HttpClient::Preconditions.assert_empty_opts(opts)")
      }

      val requestBuilder = new StringBuilder()
      requestBuilder.append("@client.request(\"" + rubyPath + "\")")

      if (hasQueryParams) {
        requestBuilder.append(".with_query(query)")
      }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        sb.append("        HttpClient::Preconditions.assert_class(hash, Hash)")
        requestBuilder.append(".with_json(hash.to_json)")
      }
      requestBuilder.append(s".${op.method.toLowerCase}")

      val responseBuilder = new StringBuilder()

      // TODO: match on all response codes
      op.responses.headOption.map { response =>
        response.datatype match {
          case Datatype.Unit.name => {
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

  def modelClassName(name: String): String = {
    Text.underscoreToInitCap(name)
  }

  def generateModel(model: core.Model): String = {
    val className = modelClassName(Text.singular(model.name))

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

    sb.append("        HttpClient::Preconditions.assert_empty_opts(opts)")
    sb.append("      end\n")
    sb.append("    end")

    sb.mkString("\n")
  }

  private def parseArgument(field: Field): String = {
    val value = if (field.default.isEmpty) {
      s"opts.delete(:${field.name})"
    } else if (field.datatype == Datatype.String) {
      s"opts.delete(:${field.name}) || \'${field.default.get}\'"
    } else if (field.datatype == Datatype.Boolean) {
      s"opts.has_key?(:${field.name}) ? (opts.delete(:${field.name}) ? true : false) : ${field.default.get}"
    } else {
      s"opts.delete(:${field.name}) || ${field.default.get}"
    }

    val hasValue = (field.required || !field.default.isEmpty)
    val assertMethod = if (hasValue) { "assert_class" } else { "assert_class_or_nil" }
    val klass = rubyClass(field.datatype)

    if (field.datatype == Datatype.Decimal) {
      if (hasValue) {
        s"BigDecimal.new(HttpClient::Preconditions.check_not_nil(${value}, '${field.name} is required').to_s)"
      } else {
        s"HttpClient::Helper.to_big_decimal_or_nil(${value})"
      }
    } else {
      s"HttpClient::Preconditions.${assertMethod}(${value}, ${klass})"
    }
  }

  private def rubyClass(datatype: Datatype): String = {
    datatype match {
      case Datatype.String => "String"
      case Datatype.Long => "Integer"
      case Datatype.Integer => "Integer"
      case Datatype.Boolean => "String"
      case Datatype.Decimal => "BigDecimal"
      case Datatype.Unit => "nil"
      case _ => {
        sys.error(s"Cannot map data type[${datatype}] to ruby class")
      }
    }
  }


}
