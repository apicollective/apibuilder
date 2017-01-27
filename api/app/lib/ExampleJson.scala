package lib

import java.util.UUID

import com.bryzek.apidoc.spec.v0.models._
import org.joda.time.{DateTime, LocalDate}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
  
case object ExampleJson {
  val TrueStrings = Seq("t", "true", "y", "yes", "on", "1", "trueclass")
  val FalseStrings = Seq("f", "false", "n", "no", "off", "0", "falseclass")

  def allFields(service: Service): ExampleJson = ExampleJson(service, Selection.All)
  def requiredFieldsOnly(service: Service): ExampleJson = ExampleJson(service, Selection.RequiredFieldsOnly)
}

trait Selection
object Selection {
  case object RequiredFieldsOnly extends Selection
  case object All extends Selection
}

case class UnknownType(typ: String) extends Throwable

case class ExampleJson(service: Service, selection: Selection) {

  def sample(typ: String): Option[JsValue] = {
    try {
      Some(
        mockValue(TextDatatype.parse(typ))
      )
    } catch {
      case UnknownType(_) => None
      case ex: Throwable => throw new RuntimeException(ex)
    }
  }

  private[this] def makeEnum(enum: Enum): JsString = {
    JsString(
      enum.values.headOption.map(_.toString).getOrElse("undefined")
    )
  }

  private[this] def makeModel(model: Model): JsValue = {
    Json.toJson(
      Map(
        model.fields.
          filter { f => selection == Selection.All || f.required }.
          map { field =>
            (field.name, mockValue(field))
          }: _*
      )
    )
  }

  private[this] def makeUnion(union: Union): JsValue = {
    union.types.headOption match {
      case None => Json.obj()
      case Some(typ) =>
        val value: JsObject = mockValue(TextDatatype.parse(typ.`type`)) match {
          case obj: JsObject => obj                // objects are serialized as is
          case jsval => Json.obj("value" -> jsval) // arrays and primitives are wrapped in a 'value' field
        }
        union.discriminator.fold {
          Json.obj(typ.`type` -> value)
        }{ discriminator =>
          Json.obj(discriminator -> JsString(typ.`type`)) ++ value
        }
    }
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
                  case None => throw new UnknownType(typ)
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
    val types = TextDatatype.parse(field.`type`)
    types.toList match {
      case Nil => JsNull
      case TextDatatype.Singleton(one) :: Nil => singleton(field)
      case TextDatatype.Singleton(_) :: _ => sys.error("Singleton must be leaf")
      case TextDatatype.List :: rest => {
        field.default match {
          case None => {
            Json.toJson(Seq(mockValue(rest)))
          }
          case Some(default) => {
            try {
              Json.parse(default).as[JsArray]
            } catch {
              case _: Throwable => Json.toJson(Seq(mockValue(rest)))
            }
          }
        }
      }
      case TextDatatype.Map :: rest => {
        field.default match {
          case None => {
            Json.obj("foo" -> mockValue(rest))
          }
          case Some(default) => {
            try {
              Json.parse(default).as[JsObject]
            } catch {
              case _: Throwable => Json.obj("foo" -> mockValue(rest))
            }
          }
        }
      }
    }
  }

  private[this] def singleton(field: Field): JsValue = {
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
                  case None => throw new UnknownType(field.`type`)
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
      case Primitives.String => JsString(randomString)
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

  private[this] def randomString(): String = {
    "lorem ipsum " + TokenGenerator.generate(6)
  }

}
