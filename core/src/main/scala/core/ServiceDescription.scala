package core

import play.api.libs.json._

object ServiceDescription {

  def apply(apiJson: String): ServiceDescription = {
    val jsValue = Json.parse(apiJson)
    ServiceDescription(jsValue)
  }

  def apply(json: JsValue): ServiceDescription = {
    val internal = InternalServiceDescription(json)
    ServiceDescription(internal)
  }

}

case class ServiceDescription(internal: InternalServiceDescription) {

  // TODO: Turn into a map
  val canonicalFieldsByResourceName = internal.resources.map { ir =>
    (ir.name -> ir.fields.filter { !_.datatype.isEmpty })
  }

  lazy val resources = internal.resources.map { Resource(_) }
  lazy val baseUrl = internal.baseUrl.getOrElse { sys.error("Missing base_url") }
  lazy val name = internal.name.getOrElse { sys.error("Missing name") }
  lazy val basePath = internal.basePath
  lazy val description = internal.description

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

object Operation {

  def apply(fields: Seq[Field], internal: InternalOperation): Operation = {
    val namedParameters = internal.namedParameters.map { paramName =>
      fields.find { _.name == paramName }.getOrElse {
        sys.error(s"Could not find field definittion for parameter named[$paramName]")
      }
    }

    Operation(method = internal.method.getOrElse { sys.error("Missing method") },
              path = internal.path,
              description = internal.description,
              parameters = namedParameters ++ internal.parameters.map { Field(fields, _) },
              responses = internal.responses.map { Response(_) })
  }

}

case class Field(name: String,
                 datatype: Datatype,
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

// TODO: Rename resource to datatype
case class Response(code: Int,
                    resource: String,
                    multiple: Boolean = false)

object Resource {

  def apply(ir: InternalResource): Resource = {
    val fields = ir.fields.map { Field(Seq.empty, _) }

    Resource(name = ir.name,
             path = ir.path,
             description = ir.description,
             fields = fields,
             operations = ir.operations.map { Operation(fields, _) })
  }

}

object Response {

  def apply(ir: InternalResponse): Response = {
    val wd = WrappedDatatype(ir.datatype.get)
    Response(code = ir.code.toInt,
             resource = wd.datatype.name,
             multiple = wd.multiple)
  }

}

case class WrappedDatatype(datatype: Datatype, multiple: Boolean)

object WrappedDatatype {

  private val ArrayRx = """^\[(.+)\]$""".r

  def apply(value: String): WrappedDatatype = {
    // TODO: Parse ir.datatype properly
    //val multiple = ArrayRx.matches(ir.datatype)
    val datatype = Datatype.findByName(value).getOrElse {
      sys.error(s"Invalid datatype[${value}]")
    }
    WrappedDatatype(datatype = datatype, multiple = false)
  }

}

sealed abstract class Datatype(val name: String)

// TODO:
// abstract def isValid(value: String): Boolean

object Datatype {

  case object Boolean extends Datatype("boolean")
  case object Decimal extends Datatype("decimal")
  case object Integer extends Datatype("integer")
  case object Long extends Datatype("long")
  case object String extends Datatype("string")
  case object Unit extends Datatype("unit")

  val All = Seq(Boolean, Decimal, Integer, Long, String, Unit)

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

  case object DateTimeIso8601 extends Format(
    name = "date-time-iso-8601",
    example = "2014-04-29T11:56:52Z",
    description = "Date time format in ISO 8601")

  val All = Seq(Uuid, DateTimeIso8601)

  def apply(name: String): Option[Format] = {
    All.find { _.name == name.toLowerCase }
  }

}


object Field {

  def apply(fields: Seq[Field], internal: InternalField): Field = {
    fields.foreach { f => println(f) }

    val datatype = internal.datatype match {
      case Some(t: String) => {
        Datatype.findByName(t).getOrElse {
          sys.error(s"Invalid datatype[${t}]")
        }
      }

      case None => {
        val ref = internal.references.getOrElse {
          sys.error("Missing datatype and/or reference for field: " + internal)
        }

        val referencedField = fields.find { _.name == ref.field.get }.getOrElse {
          sys.error(s"Reference not found[${ref.label}]. Fields: " + fields.map(_.name).mkString(" "))
        }

        referencedField.datatype
      }
    }

    internal.default.map { v => assertValidDefault(datatype, v) }

    Field(name = internal.name.get,
          datatype = datatype,
          description = internal.description,
          references = internal.references.map { Reference(_) },
          required = internal.required,
          multiple = internal.multiple,
          default = internal.default,
          minimum = internal.minimum.map(_.toLong),
          maximum = internal.maximum.map(_.toLong),
          format = internal.format.map { s => Format(s).getOrElse(sys.error(s"Invalid format[$s]")) },
          example = internal.example)
  }

  private def assertValidDefault(datatype: Datatype, value: String) {
    datatype match {
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

      case Datatype.Unit => {
        value == ""
      }

      case Datatype.String => ()

    }
  }

}

object Reference {

  def apply(internal: InternalReference): Reference = {
    Reference(internal.resource.get, internal.field.get)
  }

}
