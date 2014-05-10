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
  def models = ssd.models.map(Model(_))

  // TODO: Need to rethink client in context of operations
  def operations: Seq[Source] = ssd.operations.map(Operation(_))

  def packageName = ssd.name.toLowerCase

  override def src: String = {
    s"""package $packageName

object ${ssd.name} {

  import scala.concurrent.ExecutionContext
  import scala.concurrent.Future

  import com.ning.http.client.Realm.AuthScheme

  import play.api.libs.ws._
  import play.api.libs.ws.WS.WSRequestHolder
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  import play.api.Logger

  import java.util.UUID
  import org.joda.time.DateTime

  object Client {
    private val apiToken = sys.props.getOrElse(
      "$packageName.api.token",
      sys.error("API token must be provided")
    )

    private val apiUrl = sys.props.getOrElse(
      "$packageName.api.url",
      sys.error("API URL must be provided")
    )

    private val logger = Logger("$packageName.${ssd.name}.Client")

    def requestHolder(model: String) = {
      val url = apiUrl + model
      WS.url(url).withAuth(apiToken, "", AuthScheme.BASIC)
    }
  }

  trait Client {
    import Client._

    def model: String

    protected def requestHolder(path: String) = Client.requestHolder(model + path)

    private def logRequest(method: String, req: WSRequestHolder)(implicit ec: ExecutionContext): WSRequestHolder = {
      // auth should always be present, but just in case it isn't,
      // we'll supply a default
      val (apiToken, _, _) = req.auth.getOrElse(("", "", AuthScheme.BASIC))
      logger.info(s"curl -X $$method -u '[REDACTED]:' $${req.url}")
      req
    }

    // TODO return a Future[Response] instead of a Future[JsValue] so the code
    // generated for the operation can decide what to do with it

    private def processResponse(f: Future[Response])(implicit ec: ExecutionContext): Future[JsValue] = {
      f.map { response =>
        logger.debug(response.body)
        response.json
      }
    }

    protected def POST[T](path: String, data: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      processResponse(logRequest("POST", requestHolder(path)).post(data))
    }

    protected def GET(path: String, q: Seq[(String, String)])(implicit ec: ExecutionContext): Future[JsValue] = {
      processResponse(logRequest("GET", requestHolder(path).withQueryString(q:_*)).get())
    }

    protected def PUT[T](path: String, data: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
      processResponse(logRequest("PUT", requestHolder(path)).put(data))
    }

    protected def DELETE[T](path: String)(implicit ec: ExecutionContext): Future[JsValue] = {
      processResponse(logRequest("DELETE", requestHolder(path)).delete())
    }
  }
$body
}"""
  }

  def body: String = {
    val jsonFormatDefs = {
      val defs = ssd.models.map(JsonFormatDefs(_).src.indent(4)).mkString("\n")
      s"""
  object JsonFormats {
    implicit val jsonReadsUUID: Reads[UUID] = __.read[String].map(UUID.fromString)

    implicit val jsonWritesUUID = new Writes[UUID] {
      override def writes(value: UUID) = {
        JsString(value.toString)
      }
    }

    import org.joda.time.format.ISODateTimeFormat

    private val dateTimeFormat = ISODateTimeFormat.basicDateTime

    implicit val jsonDateTimeReads: Reads[DateTime] = {
      __.read[String].map(dateTimeFormat.parseDateTime)
    }

    implicit val jsonDateTimeWrites = new Writes[DateTime] {
      override def writes(value: DateTime) = {
        JsString(dateTimeFormat.print(value))
      }
    }
$defs
  }
  import JsonFormats._
"""
    }
    val modelDefs = models.map(_.src.indent).mkString("\n")
    jsonFormatDefs ++ modelDefs
  }

  case class Operation(operation: ScalaOperation) extends Source {
    import operation._

    def pathArg: String = {
      val tmp = "^/:".r.replaceAllIn(path, "")
      if (tmp.isEmpty) {
        "\"\""
      } else {
        if (tmp.startsWith("/")) "\"" + tmp + "\"" else "\"/\" + \"" + tmp + "\""
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
        if (param.isOption) {
          builder += s"""qBuilder ++= ${param.name}.map("${param.originalName}" -> _.toString)"""
        } else {
          builder += s"""qBuilder += "${param.originalName}" -> ${param.name}.toString"""
        }
      }
      builder.result.mkString("\n")
    }

    // TODO generate resposne types based on the responses list on the operation

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
  case class Model(model: ScalaModel) extends Source {
    import model._

    override def src: String = s"case class ${name}(${argList})"
  }

  case class JsonFormatDefs(model: ScalaModel) extends Source {
    def jsonReadsBody: String = {
      if (model.fields.size > 1) {
        val inner = model.fields.map { field =>
          val typeName = field.datatype.name
          if (field.isOption) {
            s"""(__ \\ "${field.originalName}").readNullable[${typeName}]"""
          } else {
            s"""(__ \\ "${field.originalName}").read[${typeName}]"""
          }
        }.mkString("\n     and ")
s"""
  ($inner)(${model.name})"""
      } else {
        val field = model.fields.head
s"""
  new Reads[${model.name}] {
    override def reads(json: JsValue) = {
      (json \\ "${field.originalName}").validate[${field.typeName}].map { value =>
        new ${model.name}(
          ${field.name} = value
        )
      }
    }
  }"""
      }
    }

    def jsonWritesBody: String = {
      if (model.fields.size > 1) {
        val inner = model.fields.map { field =>
          val typeName = field.datatype.name
          if (field.isOption) {
            s"""(__ \\ "${field.name}").writeNullable[${typeName}]"""
          } else {
            s"""(__ \\ "${field.name}").write[${typeName}]"""
          }
        }.mkString("\n     and ")
s"""
  ($inner)(unlift(${model.name}.unapply))"""
      } else {
        val field = model.fields.head
s"""
  new Writes[${model.name}] {
    override def writes(value: ${model.name}) = {
      Json.obj(
        "${field.originalName}" -> Json.toJson(value.${field.name})
      )
    }
  }"""
      }
    }

    override def src: String = {
s"""
// ${model.name} JSON format
implicit val jsonReads${model.name}: Reads[${model.name}] =$jsonReadsBody

implicit val jsonWrites${model.name}: Writes[${model.name}] =$jsonWritesBody
"""
    }
  }
}

