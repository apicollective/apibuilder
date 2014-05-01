package core.generator

import core._
import Text._
import ScalaUtil._

object Play2ClientGenerator {
  def apply(json: String) = {
    val sd = ServiceDescription(json)
    val ssd = new ScalaServiceDescription(sd)
    new Play2ClientGenerator(ssd).src
  }
}

class Play2ClientGenerator(ssd: ScalaServiceDescription)
extends Source
{
  def resources = ssd.resources.map(Resource(_))

  def packageName = ssd.name.toLowerCase

  override def src: String = {
    s"""package $packageName

object ${ssd.name} {

  import play.api.libs.ws._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  object Client {
    private val apiToken = sys.props.getOrElse(
      "$packageName.api.token",
      sys.error("API token must be provided")
    )

    private val apiUrl = sys.props.getOrElse(
      "$packageName.api.url",
      sys.error("API URL must be provided")
    )

    def req(resource: String) = {
      import com.ning.http.client.Realm.AuthScheme
      val url = apiUrl + resource
      WS.url(url).withAuth(apiToken, "", AuthScheme.BASIC)
    }
  }

  trait Client {
    import Client._

    def resource: String

    protected def req(path: String) = Client.req(resource + path)

    // TODO these methods need logging

    protected def POST[T](path: String, data: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      req(path).post(data).map(_.json)
    }

    protected def GET(path: String, q: Seq[(String, String)])(implicit ec: ExecutionContext): Future[JsValue] = {
      req(path).withQueryString(q:_*).get().map(_.json)
    }

    protected def PUT[T](path: String, data: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      req(path).put(data).map(_.json)
    }

    protected def DELETE[T](path: String)(implicit ec: ExecutionContext): Future[JsValue] = {
      req(path).delete().map(_.json)
    }
  }
$body
}"""
  }

  def body: String = {
    val jsonFormatDefs = {
      val defs = ssd.resources.map(JsonFormatDefs(_).src.indent(4)).mkString("\n")
      s"""
  object JsonFormats {
$defs
  }
"""
    }
    val resourceDefs = resources.map(_.src.indent).mkString("\n")
    jsonFormatDefs ++ resourceDefs
  }

  case class Operation(operation: ScalaOperation) extends Source {
    import operation._

    def pathArg: String = {
      val tmp = path.map("^/:".r.replaceAllIn(_, ""))
        .getOrElse("")
      if (tmp.isEmpty) {
        "\"\""
      } else {
        if (tmp.startsWith("/")) "\"" + tmp + "\"" else "\"/\" + " + tmp
      }
    }

    def buildPayload: String = {
      def objArgs = operation.parameters.map { param =>
        s""""${param.originalName}" -> Json.toJson(${param.name})"""
      }.mkString(",\n").indent
s"""val payload = Json.obj(
$objArgs
)"""
    }

    def buildGetArgs: String = {
      val builder = List.newBuilder[String]
      builder += "val qBuilder = List.newBuilder[(String, String)]"
      operation.parameters.foreach { param =>
        // TODO support more complicated types
        require(Set("String", "Long", "Int", "Boolean", "BigDecimal", "UUID")(param.dataType.name))
        if (param.isOption) {
          builder += s"""qBuilder ++= ${param.name}.map("${param.originalName}" -> _.toString)"""
        } else {
          builder += s"""qBuilder += "${param.originalName}" -> ${param.name}.toString"""
        }
      }
      builder.result.mkString("\n")
    }

    // TODO finish implementing method generation
    def body = method match {
      case "POST" => s"""$buildPayload
POST(
  path = $pathArg,
  payload
)"""
      case "GET" => s"""$buildGetArgs
GET(
  path = $pathArg,
  qBuilder.result
)"""
      case "PATCH" => "throw new UnsupportedOperationException // TODO support PATCH"
      case "PUT" => s"""$buildPayload
PUT(
  path = $pathArg,
  payload
)"""
      case "DELETE" => s"""DELETE($pathArg)"""
    }

    override def src: String = s"""
${description}def $name($argList)(implicit ec: ExecutionContext) = {
${body.indent}
}
"""
  }
  case class Resource(resource: ScalaResource) extends Source {
    import resource._

    def operations: Seq[Source] = resource.operations.map(Operation(_))

    override def src: String = s"""
object ${resource.name}Client extends Client {
  def resource = "$path"
$body
}
case class ${name}(${argList})
"""

    def body: String = {
      val methods = operations.map(_.src.indent).mkString("\n")
      methods
    }
  }

  case class JsonFormatDefs(resource: ScalaResource) extends Source {
    def jsonReadsBody: String = {
      if (resource.fields.size > 1) {
        val inner = resource.fields.map { field =>
          val typeName = field.dataType.name
          if (field.isOption) {
            s"""(__ \\ "${field.originalName}").readNullable[${typeName}]"""
          } else {
            s"""(__ \\ "${field.originalName}").read[${typeName}]"""
          }
        }.mkString("\n     and ")
s"""
  ($inner)(${resource.name})"""
      } else {
        val field = resource.fields.head
s"""
  new Reads[${resource.name}] {
    override def reads(json: JsValue) = {
      (json \\ "${field.originalName}").validate[${field.typeName}].map { value =>
        new ${resource.name}(
          ${field.name} = value
        )
      }
    }
  }"""
      }
    }

    def jsonWritesBody: String = {
      if (resource.fields.size > 1) {
        val inner = resource.fields.map { field =>
          val typeName = field.dataType.name
          if (field.isOption) {
            s"""(__ \\ "${field.name}").writeNullable[${typeName}]"""
          } else {
            s"""(__ \\ "${field.name}").write[${typeName}]"""
          }
        }.mkString("\n     and ")
s"""
  ($inner)(unlift(${resource.name}.unapply))"""
      } else {
        val field = resource.fields.head
s"""
  new Writes[${resource.name}] {
    override def writes(value: ${resource.name}) = {
      Json.obj(
        "${field.originalName}" -> Json.toJson(value.${field.name})
      )
    }
  }"""
      }
    }

    override def src: String = {
s"""
// ${resource.name} JSON format
implicit val jsonReads${resource.name}: Reads[${resource.name}] =$jsonReadsBody

implicit val jsonWrites${resource.name}: Writes[${resource.name}] =$jsonWritesBody
"""
    }
  }
}

