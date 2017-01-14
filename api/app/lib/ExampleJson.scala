package lib

import java.util.UUID

import com.bryzek.apidoc.spec.v0.models._
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
  
case object ExampleJson {
  val TrueStrings = Seq("t", "true", "y", "yes", "on", "1", "trueclass")
  val FalseStrings = Seq("f", "false", "n", "no", "off", "0", "falseclass")
}

case class ExampleJson(service: Service) {

  def sample(typ: String): JsValue = {
    mockValue(TextDatatype.parse(typ))
  }

  private[this] def makeEnum(enum: Enum): JsString = {
    JsString(
      enum.values.headOption.map(_.toString).getOrElse("undefined")
    )
  }

  private[this] def makeModel(model: Model): JsValue = {
    Json.toJson(
      Map(
        model.fields.map { field =>
          (field.name, mockValue(field))
        }: _*
      )
    )
  }

  private[this] def makeUnion(union: Union): JsValue = {
    val typ = union.types.headOption.getOrElse {
      sys.error("Union type[${union.qualifiedName}] does not have any times")
    }
    sample(typ.`type`)
  }

  private[this] def mockValue(types: Seq[TextDatatype]): JsValue = {
    types.toList match {
      case Nil => JsNull
      case TextDatatype.Singleton(one) :: Nil => singleton(one)
      case TextDatatype.Singleton(one) :: _ => sys.error("Singleton must be leaf")
      case TextDatatype.List :: rest => Json.toJson(Seq(mockValue(rest)))
      case TextDatatype.Map :: rest => Json.obj("foo" -> mockValue(rest))
    }
  }

  private[this] def singleton(typ: String): JsValue = {
    Primitives(typ) match {
      case None => {
        service.enums.find(_.name == typ) match {
          case Some(e) => makeEnum(e)
          case None => {
            service.models.find(_.name == typ) match {
              case Some(m) => makeModel(m)
              case None => {
                service.unions.find(_.name == typ) match {
                  case Some(u) => makeUnion(u)
                  case None => sys.error("Could not generate mock data for type[$typ]")
                }
              }
            }
          }
        }
      }

      case Some(p) => mockPrimitive(p)
    }
  }

  private[this] def mockValue(field: Field): JsValue = {
    Primitives(field.`type`) match {
      case None => {
        service.enums.find(_.name == field.`type`) match {
          case Some(e) => JsString(e.values.headOption.map(_.name).getOrElse("undefined"))
          case None => {
            service.models.find(_.name == field.`type`) match {
              case Some(m) => makeModel(m)
              case None => {
                service.unions.find(_.name == field.`type`) match {
                  case Some(u) => makeUnion(u)
                  case None => sys.error("Could not generate mock data for type[$typ]")
                }
              }
            }
          }
        }
      }

      case Some(p) => {
        field.example match {
          case None => mockPrimitive(p)
          case Some(ex) => primitiveExample(p, ex)
        }
      }
    }
  }

  private[this] def mockPrimitive(p: Primitives): JsValue = {
    p match {
      case Primitives.Boolean => JsBoolean(true)
      case Primitives.Double => JsNumber(1.0)
      case Primitives.Integer => JsNumber(1)
      case Primitives.Long => JsNumber(1)
      case Primitives.DateIso8601 => {
        val now = DateTime.now
        JsString(s"${now.year}-${now.monthOfYear()}-${now.dayOfMonth()}")
      }
      case Primitives.DateTimeIso8601 => JsString(ISODateTimeFormat.dateTime.print(DateTime.now))
      case Primitives.Decimal => Json.toJson(BigDecimal("1"))
      case Primitives.String => JsString(UUID.randomUUID.toString.replaceAll("-", " "))
      case Primitives.Object => Json.obj("foo" -> "bar")
      case Primitives.Unit => JsNull
      case Primitives.Uuid => JsString(UUID.randomUUID.toString)
    }
  }

  private[this] def primitiveExample(p: Primitives, ex: String): JsValue = {
    p match {
      case Primitives.Boolean => JsBoolean(parseBoolean(ex, true))
      case Primitives.Double => JsNumber(parseDouble(ex, 1.0))
      case Primitives.Integer => JsNumber(parseInt(ex, 1))
      case Primitives.Long => JsNumber(parseInt(ex, 1))
      case Primitives.DateIso8601 => {
        val ts = parseDate(ex, LocalDate.now)
        JsString(s"${ts.year}-${ts.monthOfYear()}-${ts.dayOfMonth()}")
      }
      case Primitives.DateTimeIso8601 => {
        val ts = parseDateTime(ex, DateTime.now)
        JsString(ISODateTimeFormat.dateTime.print(ts))
      }
      case Primitives.Decimal => Json.toJson(BigDecimal(parseDouble(ex, 1)))
      case Primitives.String => JsString(ex)
      case Primitives.Object => parseObject(ex, Json.obj("foo" -> "bar"))
      case Primitives.Unit => JsNull
      case Primitives.Uuid => JsString(parseUUID(ex, UUID.randomUUID).toString)
    }
  }

  private[this] def parseBoolean(value: String, default: Boolean): Boolean = {
    if (ExampleJson.TrueStrings.contains(value.toLowerCase().trim)) {
      true
    } else if (ExampleJson.TrueStrings.contains(value.toLowerCase().trim)) {
      false
    } else {
      default
    }
  }

  private[this] def parseDouble(value: String, default: Double): Double = {
    try {
      value.toDouble
    } catch {
      case _: Throwable => default
    }
  }

  private[this] def parseInt(value: String, default: Int): Int = {
    try {
      value.toInt
    } catch {
      case _: Throwable => default
    }
  }

  private[this] def parseUUID(value: String, default: UUID): UUID = {
    try {
      UUID.fromString(value)
    } catch {
      case _: Throwable => default
    }
  }

  private[this] def parseObject(value: String, default: JsObject): JsObject = {
    try {
      Json.parse(value).as[JsObject]
    } catch {
      case _: Throwable => default
    }
  }

  private[this] def parseDate(value: String, default: LocalDate): LocalDate = {
    try {
      ISODateTimeFormat.dateTimeParser.parseLocalDate(value)
    } catch {
      case _: Throwable => default
    }
  }

  private[this] def parseDateTime(value: String, default: DateTime): DateTime = {
    try {
      ISODateTimeFormat.dateTimeParser.parseDateTime(value)
    } catch {
      case _: Throwable => default
    }
  }
}
