package lib

import core.{ Datatype, Field, ServiceDescription, Resource }
import java.io.File

/**
 * Generates a Play routes file based on the service description
 * from api.json
 */
case class RubyGemGenerator(service: ServiceDescription) {

  private val moduleName = Text.safeName(service.name)
  private val moduleKey = UrlKey.generate(moduleName)

  def generate(): String = {
    val baseDir = new File("/tmp/ruby_gem")
    val libDir = new File(baseDir, "lib")
    val moduleDir = new File(libDir, moduleKey)
    val resourceDir = new File(moduleDir, "resources")
    resourceDir.mkdirs

    val clientDir = new File(moduleDir, "client")
    clientDir.mkdirs

    service.resources.foreach { r =>
      val resourceFilename = s"${Text.singular(r.name)}.rb"
      writeToFile(new File(resourceDir, resourceFilename), generateResource(r))

      val clientFilename = s"${r.name}.rb"
      writeToFile(new File(clientDir, clientFilename), generateClient(r))
      println(generateClient(r))
    }

    baseDir.toString
  }

  def generateClient(resource: core.Resource): String = {
    val sb = scala.collection.mutable.ListBuffer[String]()

    sb.append("      def initialize(client)")
    sb.append(s"        @client = Apidoc::Preconditions.assert_class(client, Apidoc::Client)")
    sb.append("      end")

    resource.operations.foreach { op =>
      val path = resource.path + op.path.getOrElse("")

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

      val paramStrings = scala.collection.mutable.ListBuffer[String]()
      pathParams.map(_.name).foreach { n => paramStrings.append(n) }

      val hasQueryParams = (!GeneratorUtil.isJsonDocumentMethod(op.method) && !otherParams.isEmpty)
      if (hasQueryParams) {
        paramStrings.append("opts={}")
      }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        paramStrings.append("json_document")
      }

      sb.append("")
      op.description.map { desc =>
        sb.append(formatComment(desc, 6))
      }
      sb.append(s"      def ${methodName}(" + paramStrings.mkString(", ") + ")")

      pathParams.foreach { param =>
        val klass = rubyClass(param.dataType)
        sb.append(s"        Apidoc::Preconditions.assert_class(${param.name}, ${klass})")
      }

      if (hasQueryParams) {
        val paramBuilder = scala.collection.mutable.ListBuffer[String]()

        otherParams.foreach { param =>
          paramBuilder.append(s":${param.name} => ${parseArgument(param)}")
        }

        sb.append("        query = {")
        sb.append("          " + paramBuilder.mkString(",\n          "))
        sb.append("        }.compact")
        sb.append(s"        Apidoc::Preconditions.assert_empty_opts(opts)")
      }

      val requestBuilder = new StringBuilder()
      requestBuilder.append("Apidoc::Request.new(\"" + rubyPath + "\")")

      if (hasQueryParams) {
        requestBuilder.append(".with_query(query)")
      }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        sb.append("        Apidoc::Preconditions.assert_class(json_document, String)")
        sb.append("        Apidoc::Preconditions.assert_not_blank(json_document, \"json_document cannot be blank\")")
        requestBuilder.append(s".${op.method.toLowerCase}(json_document)")
      } else {
        requestBuilder.append(s".${op.method.toLowerCase}()")
      }

      val responseBuilder = new StringBuilder()
      // TODO: match on all response codes
      op.responses.headOption.map { response =>
        response.resource match {
          case None => {
            // TODO: match on response code
            responseBuilder.append("\n        nil")
          }

          case Some(resourceName: String) => {
            if (op.responses.head.multiple) {
              responseBuilder.append(".map")
            }
            responseBuilder.append(s" { hash => ${moduleName}::Resources::${Text.underscoreToInitCap(resourceName)}.new(hash) } ")
          }
        }
      }

      sb.append(s"        ${requestBuilder.toString}${responseBuilder.toString}")
      sb.append("      end")
    }

    wrap("Clients", Text.underscoreToInitCap(resource.name), resource.description, sb.mkString("\n"))
  }

  def generateResource(resource: core.Resource): String = {
    val resourceNameSingular = Text.singular(resource.name)
    val className = Text.underscoreToInitCap(resourceNameSingular)

    val sb = scala.collection.mutable.ListBuffer[String]()

    sb.append("      attr_reader " + resource.fields.map( f => s":${f.name}" ).mkString(", "))

    sb.append("")
    sb.append("      def initialize(opts={})")

    resource.fields.map { field =>
      sb.append(s"        @${field.name} = ${parseArgument(field)}")
    }

    sb.append("        Apidoc::Preconditions.check_empty_opts(opts)")
    sb.append("      end")


    wrap("Resources", className, None, sb.mkString("\n"))
  }

  // Format into a multi-line comment w/ a set number of spaces for
  // leading indentation
  private def formatComment(comment: String, numberSpaces: Int): String = {
    val maxLineLength = 80 - 2 - numberSpaces
    val sb = new StringBuilder()
    var currentWord = new StringBuilder()
    comment.split(" ").foreach { word =>
      if (word.length + currentWord.length >= maxLineLength) {
        if (!currentWord.isEmpty) {
          if (!sb.isEmpty) {
            sb.append("\n")
          }
          sb.append((" " * numberSpaces)).append("#").append(currentWord.toString)
        }
        currentWord = new StringBuilder()
      } else {
        currentWord.append(" ").append(word)
      }
    }
    if (!currentWord.isEmpty) {
      if (!sb.isEmpty) {
        sb.append("\n")
      }
      sb.append((" " * numberSpaces)).append("#").append(currentWord.toString)
    }
    sb.toString
  }

  private def wrap(submoduleName: String, className: String, comments: Option[String], body: String): String = {
    val classWithComments = comments match {
      case None => s"    class ${className}"
      case Some(c: String) => formatComment(c, 4) + s"\n    class ${className}"
    }

    Seq(
      s"module ${moduleName}",
      s"  module ${submoduleName}",
      classWithComments,
      body,
      "    end",
      "  end",
      "end"
    ).mkString("\n\n")
  }

  private def parseArgument(field: Field): String = {
    val value = if (field.default.isEmpty) {
      s"opts.delete(:${field.name})"
    } else if (field.dataType == Datatype.String) {
      s"opts.delete(:${field.name}) || \'${field.default.get}\'"
    } else if (field.dataType == Datatype.Boolean) {
      s"opts.has_key?(:${field.name}) ? (opts.delete(:${field.name}) ? true : false) : ${field.default.get}"
    } else {
      s"opts.delete(:${field.name}) || ${field.default.get}"
    }

    val assertMethod = if (field.required) { "assert_class" } else { "assert_class_or_nil" }
    val klass = rubyClass(field.dataType)
    s"Apidoc::Preconditions.${assertMethod}(${value}, ${klass})"
  }

  private def rubyClass(dataType: Datatype): String = {
    dataType match {
      case Datatype.String => "String"
      case Datatype.Long => "Integer"
      case Datatype.Integer => "Integer"
      case Datatype.Boolean => "String"
      case _ => {
        sys.error(s"Cannot map data type[${dataType}] to ruby class")
      }
    }
  }


  private def writeToFile(file: File, contents: String) {
    val fstream = new java.io.FileWriter(file)
    val out = new java.io.BufferedWriter(fstream)
    out.write(contents)
    out.close()
  }

}

/*
module IrisHub
  module Resources

    class Vendors < IrisHub::RestClient

      def initialize(client)
        super(client, "/vendors")
      end

      def get(params={})
        new_request(GET).with_query(params).get.map { |v| from_hash(v) }
      end

      def put(json)
        new_request(PUT).with_json(json).get { |v| from_hash(v) }
      end

      def post(json)
        new_request(POST).with_json(json).get { |v| from_hash(v) }
      end

      private
      def from_hash(hash)
        Vendor.new(hash['guid'], hash['name'])
      end

    end

  end
end
*/
