package core

import play.api.libs.json._

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescription(jsValue)
  }

}

/**
 * Parses api.json file into a set of case classes
 */
case class ServiceDescription(json: JsValue) {

  lazy val resources: Seq[Resource] = {
    (json \ "resources").as[JsObject].fields.map { v =>
      v match {
        case(key, value) => Resource.parse(key, value.as[JsObject])
      }
    }
  }

  lazy val baseUrl = (json \ "base_url").as[String]
  lazy val basePath = (json \ "base_path").asOpt[String]
  lazy val name = (json \ "name").as[String]
  lazy val description = (json \ "description").asOpt[String]

}

case class Resource(name: String,
                    path: String,
                    description: Option[String],
                    fields: Seq[Field],
                    operations: Seq[Operation])

case class Operation(method: String,
                     path: Option[String],
                     description: Option[String],
                     parameters: Seq[Field],
                     responses: Seq[Response])

case class Field(name: String,
                 dataType: Datatype,
                 description: Option[String] = None,
                 required: Boolean = true,
                 multiple: Boolean = false,
                 format: Option[Format] = None,
                 references: Option[Reference] = None,
                 default: Option[String] = None,
                 example: Option[String] = None,
                 minimum: Option[Long] = None,
                 maximum: Option[Long] = None)

case class Reference(resource: String, field: String) {

  lazy val label = s"$resource.$field"

}


case class Response(code: Int,
                    resource: Option[String] = None,
                    multiple: Boolean = false)

object Resource {

  def parse(name: String, value: JsObject): Resource = {
     val path = (value \ "path").asOpt[String].getOrElse( s"/${name}" )
     val description = (value \ "description").asOpt[String]

     val fields = (value \ "fields").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json => Field(json.as[JsObject]) }
       }

     }

     val operations = (value \ "operations").asOpt[JsArray] match {

       case None => Seq.empty

       case Some(a: JsArray) => {
         a.value.map { json =>

           val parameters = (json \ "parameters").asOpt[JsArray] match {
             case None => Seq.empty
             case Some(a: JsArray) => {
               a.value.map { data => Field(data.as[JsObject]) }
             }
           }

           val responses = (json \ "responses").asOpt[JsArray] match {
             case None => Seq.empty
             case Some(a: JsArray) => {
               a.value.map { data => Response.parse(data.as[JsObject]) }
             }
           }

           Operation(method = (json \ "method").as[String],
                     path = (json \ "path").asOpt[String],
                     description = (json \ "description").asOpt[String],
                     responses = responses,
                     parameters = parameters)
         }
       }

    }

    Resource(name = name,
             path = path,
             description = description,
             fields = fields,
             operations = operations)
  }

}

object Response {

  def parse(json: JsObject): Response = {
    val code = (json \ "code").as[Int]
    (json \ "result") match {
      case (v: JsUndefined) => {
        Response(code = code)
      }

      case v: JsString => {
        Response(code = code,
                 resource = Some(v.value),
                 multiple = false)
      }

      case v: JsArray => {
        assert(v.value.size == 1,
               "When an array, response must contain exactly 1 element: %s".format(v.value.mkString(", ")))
        Response(code = code,
                 resource = Some(v.value.head.as[JsString].value),
                 multiple = true)
      }

      case v: Any => {
        sys.error(s"Unhandled response value[$v]")
      }
    }
  }
}

sealed abstract class Datatype(val name: String)

object Datatype {

  case object String extends Datatype("string")
  case object Integer extends Datatype("integer")
  case object Long extends Datatype("long")
  case object Boolean extends Datatype("boolean")
  case object Decimal extends Datatype("decimal")
  case class UserType(override val name: String) extends Datatype(name)

  val All = Seq(String, Integer, Long, Boolean, Decimal)

  def findByName(name: String): Option[Datatype] = {
    All.find { dt => dt.name == name }
  }

}

sealed abstract class Format(val name: String, val example: String, val description: String)

object Format {

  case object Uuid extends Format(
    name = "uuid",
    example = "5ecf6502-e532-4738-aad5-7ac9701251dd",
    description = "String representation of a universally unique identifier (UUID)")

  case object DateTime extends Format(
    name = "date-time",
    example = "2014-04-29T11:56:52Z",
    description = "Date time format in ISO 8601")

  val All = Seq(Uuid, DateTime)

  def apply(name: String): Option[Format] = {
    All.find { _.name == name.toLowerCase }
  }

}


object Field {

  def apply(json: JsObject): Field = {
    val datatypeName = (json \ "type").as[String]
    val datatype = Datatype.findByName(datatypeName).getOrElse {
      new Datatype.UserType(datatypeName)
    }

    val default = asOptString(json, "default")
    default.map { v => assertValidDefault(datatype, v) }

    Field(name = (json \ "name").as[String],
          dataType = datatype,
          description = (json \ "description").asOpt[String],
          references = (json \ "references").asOpt[String].map { Reference(_) },
          required = (json \ "required").asOpt[Boolean].getOrElse(true),
          multiple = (json \ "multiple").asOpt[Boolean].getOrElse(false),
          default = default,
          minimum = (json \ "minimum").asOpt[Long],
          maximum = (json \ "maximum").asOpt[Long],
          format = (json \ "format").asOpt[String].map( s =>
            Format(s).getOrElse {
              sys.error(s"Invalid format[$s]")
            }
          ),
          example = asOptString(json, "example"))
  }

  private def asOptString(json: JsValue, field: String): Option[String] = {
    (json \ field) match {
      case (_: JsUndefined) => None
      case (v: JsValue) => Some(v.toString)
    }
  }

  private def assertValidDefault(dataType: Datatype, value: String) {
    dataType match {
      case Datatype.Boolean => {
        if (value != "true" && value != "false") {
          sys.error(s"defaults for boolean fields must be the string true or false and not[${value}]")
        }
      }

      case Datatype.Integer => {
        value.toInt
      }

      case Datatype.Long => {
        value.toLong
      }

      case Datatype.Decimal => {
        BigDecimal(value)
      }

      case Datatype.String => ()

      case _: Datatype.UserType =>
        sys.error("Defaults not supported for user defined types.")
    }
  }

}

object Reference {

  def apply(value: String): Reference = {
    val parts = value.split("\\.")
    require(parts.length == 2,
            s"Invalid reference[${value}]. Expected <resource name>.<field name>")
    Reference(parts.head, parts.last)
  }

}
