package lib

import core.{ ServiceDescription, Resource }
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

    service.resources.find { r => r.name == "users" }.foreach { r =>
      val resourceFilename = s"${Text.singular(r.name)}.rb"
      writeToFile(new File(resourceDir, resourceFilename), generateResource(r))
      println(generateResource(r))

      val clientFilename = s"${r.name}.rb"
      writeToFile(new File(resourceDir, clientFilename), generateClient(r))
      println(generateClient(r))
    }

    baseDir.toString
  }

  def generateClient(resource: core.Resource): String = {
    val sb = scala.collection.mutable.ListBuffer[String]()

    sb.append("      def initialize(client, uri)")
    sb.append(s"        @client = Apidoc::Preconditions.assert_class(client, Apidoc::Client)")
    sb.append(s"        @uri = Apidoc::Preconditions.assert_class(uri, String)")
    sb.append("      end")

    sb.append("")
    sb.append("      def new_request(verb, path=nil)")
    sb.append("        Preconditions.assert_class(verb, Apidoc::HttpVerb)")
    sb.append("        Preconditions.assert_class_or_nil(path, String)")
    sb.append("")
    sb.append("        request = ApidocRequest.new(@uri + path.to_s, verb)")
    sb.append("        request.with_auth(@client.authorization.token)")
    sb.append("      end")

    resource.operations.foreach { op =>
      val methodName = op.method.toLowerCase + op.path.getOrElse("").split("/").mkString("_")

      val path = resource.path + op.path.getOrElse("")
      val namedParams = GeneratorUtil.namedParametersInPath(path)
      val pathParams = op.parameters.filter { p => namedParams.contains(p.name) }
      val otherParams = op.parameters.filter { p => !namedParams.contains(p.name) }

      val paramString = new StringBuilder()
      paramString.append(pathParams.map(_.name).mkString(", "))

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
        if (!pathParams.isEmpty) {
          paramString.append(", ")
        }
        // TODO: Check for collision on the name json
        paramString.append("json")
      } else if (!otherParams.isEmpty) {
        if (!pathParams.isEmpty) {
          paramString.append(", ")
        }
        paramString.append("opts={}")
      }

      sb.append("")
      sb.append(s"      def ${methodName}(" + paramString.toString + ")")

      pathParams.foreach { param =>
        val klass = rubyClass(param.dataType)
        sb.append(s"        Apidoc::Preconditions.assert_class(${param.name}, ${klass})")
      }

      if (GeneratorUtil.isJsonDocumentMethod(op.method)) {
          sb.append(s"        Apidoc::Preconditions.assert_class_or_nil(json, String)")

      } else {
        otherParams.foreach { param =>
          val klass = rubyClass(param.dataType)
          sb.append(s"        ${param.name} = Apidoc::Preconditions.assert_class_or_nil(opts.delete(:${param.name}), ${klass})")
          if (param.required) {
          sb.append(s"        Apidoc::Preconditions.check_not_null(${param.name}, '${param.name} is required')")
          }
        }
      }

      sb.append("      end")
    }

    wrap("Clients", Text.underscoreToInitCap(resource.name), sb.mkString("\n"))
  }

  def wrap(submoduleName: String, className: String, body: String): String = {
    Seq(
      s"module ${moduleName}",
      s"  module ${submoduleName}",
      s"    class ${className}",
      body,
      "    end",
      "  end",
      "end"
    ).mkString("\n\n")
  }


  def generateResource(resource: core.Resource): String = {
    val resourceNameSingular = Text.singular(resource.name)

    val sb = scala.collection.mutable.ListBuffer[String]()

    sb.append("      attr_reader " + resource.fields.map( f => s":${f.name}" ).mkString(", "))

    sb.append("")
    sb.append("      def initialize(opts={})")

    resource.fields.map { field =>
      val klass = rubyClass(field.dataType)

      sb.append(s"        @${field.name} = opts.delete(:${field.name})")

      if (!field.default.isEmpty) {
        if (klass == "String") {
          sb.append(s"        @${field.name} ||= \'#{field.default}\'")
        } else {
          sb.append(s"        @${field.name} ||= #{field.default}")
        }

      }

      sb.append(s"        Apidoc::Preconditions.assert_class_or_nil(@${field.name}, ${klass})")

      if (field.required) {
        sb.append(s"        Apidoc::Preconditions.check_not_nil(@${field.name}, \'${field.name} is required\')")
      }

      sb.append("")

    }


    sb.append("        Apidoc::Preconditions.check_empty_opts(opts)")
    sb.append("      end")

    wrap("Resources", Text.underscoreToInitCap(resourceNameSingular), sb.mkString("\n"))
  }

  private def rubyClass(dataType: String): String = {
    dataType match {
      case "string" => "String"
      case "long" => "Integer"
      case "integer" => "Integer"
      case "boolean" => "String"
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
