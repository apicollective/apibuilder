package models

import lib.UrlKey
import db.{ Organization, ServiceDao, VersionDao }
import scala.io.Source
import java.io.File
import play.api.libs.json._

case class Service(org: Organization, serviceDao: ServiceDao, version: VersionDao) {

  private lazy val parser = JsonParser(version.json)

  lazy val resources: Seq[Resource] = {
    parser.getValue(parser.json, "resources").as[JsObject].fields.map { v =>
      v match {
        case(key, value) => Resource.parse(parser, key, value.as[JsObject])
      }
    }
  }

  lazy val name = serviceDao.name
  lazy val key = serviceDao.key
  lazy val baseUrl = parser.getValue(parser.json, "base_url").as[String]
  lazy val jsonName = parser.getValue(parser.json, "name").as[String]
  lazy val description = parser.getOptionalValue(parser.json, "description").map(v => v.as[String])

  lazy val allVersions: Seq[VersionDao] = VersionDao.findAllByService(serviceDao)

  def resource(key: String): Option[Resource] = {
    resources.find { r => r.key == key }
  }

  def fullUrl(url: String): String = {
    s"${baseUrl}$url"
  }

  def url(resource: Resource, op: Operation): String = {
    op.path match {
      case None => resource.path
      case Some(o) => s"${resource.path}${o}"
    }
  }

  def label(resource: Resource, operation: Operation): String = {
    "%s %s".format(operation.method, url(resource, operation))
  }

}

case class Resource(name: String,
                    path: String,
                    description: Option[String],
                    fields: Seq[Field], operations: Seq[Operation]) {

  lazy val key: String = UrlKey.generate(name)

  def field(name: String): Option[Field] = {
    fields.find { f => f.name == name }
  }

  def operation(method: String, path: Option[String] = None): Option[Operation] = {
    operations.find { op => op.method == method && op.path == path }
  }

}

case class Operation(method: String,
                     path: Option[String],
                     description: Option[String],
                     parameters: Seq[Field],
                     response: Response)

case class Field(name: String,
                 dataType: String,
                 description: Option[String] = None,
                 required: Boolean = true,
                 format: Option[String] = None,
                 references: Option[String] = None,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Int] = None,
                 maximum: Option[Int] = None)

case class Response(code: Int,
                    resource: Option[String] = None,
                    multiple: Boolean = false)

object Resource {

  def parse(parser: JsonParser, name: String, value: JsObject): Resource = {
     val path = parser.getValue(value, "path").as[JsString].value
     val description = parser.getOptionalValue(value, "description").map(_.as[JsString].value)
     val fields = parser.getArray(value, "fields").map { json => Field.parse(parser, json.as[JsObject]) }

     val operations = parser.getArray(value, "operations").map { json =>
      Operation(method = parser.getValue(json, "method").as[JsString].value,
                path = parser.getOptionalValue(json, "path").map(_.as[JsString].value),
                description = parser.getOptionalValue(json, "description").map(_.as[JsString].value),
                response = Response.parse(parser, json.as[JsObject]),
                parameters = parser.getArray(json, "parameters").map { data => Field.parse(parser, data.as[JsObject]) })
    }

    Resource(name = name,
             path = path,
             description = description,
             fields = fields,
             operations = operations)
  }

}

object Response {

  def parse(parser: JsonParser, json: JsObject): Response = {
    parser.getOptionalValue(json, "response_code") match {

      case Some(v) => {
        Response(code = v.as[JsNumber].value.toInt)
      }

      case None => {
        parser.getValue(json, "response") match {

          case v: JsArray => {
            assert(v.value.size == 1,
                   "When an array, response must contain exactly 1 element: %s".format(v.value.mkString(", ")))
            Response(code = 200,
                     resource = Some(v.value.head.as[JsString].value),
                     multiple = true)
          }

          case v: JsString => {
            Response(code = 200,
                     resource = Some(v.value))
          }

          case v: Any => {
            sys.error(s"Could not parse response: $v")
          }
        }

      }

    }
  }

}


object Field {

  def parse(parser: JsonParser, json: JsObject): Field = {
    Field(name = parser.getValue(json, "name").as[JsString].value,
          dataType = parser.getValue(json, "type").as[JsString].value,
          description = parser.getOptionalValue(json, "description").map(_.as[JsString].value),
          references = parser.getOptionalValue(json, "references").map(_.as[JsString].value),
          required = parser.getOptionalValue(json, "required").map(_.as[JsBoolean].value).getOrElse(true),
          default = parser.getOptionalValue(json, "default").map(_.toString),
          minimum = parser.getOptionalValue(json, "minimum").map(_.as[Int]),
          maximum = parser.getOptionalValue(json, "maximum").map(_.as[Int]),
          format = parser.getOptionalValue(json, "format").map(_.as[JsString].value),
          example = parser.getOptionalValue(json, "example").map(_.as[JsString].value))
  }
}
