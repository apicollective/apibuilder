package core

import lib.{PrimitiveMetadata, Primitives}
import java.util.UUID
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException
import com.gilt.apidocgenerator.models.{Container, Datatype, Type, TypeKind}
import scala.util.{Failure, Success, Try}

case class TypeValidatorEnums(name: String, values: Seq[String])

case class TypeValidator(
  enums: Seq[TypeValidatorEnums] = Seq.empty
) {

  private[this] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()

  private def parseJsonOrNone(value: String): Option[JsValue] = {
    Try(Json.parse(value)) match {
      case Success(jsValue) => Some(jsValue)
      case Failure(ex) => ex match {
        case e: JsonParseException => None
        case e: JsonMappingException => None
        case e: JsonProcessingException => None
        case e: Throwable => throw e
      }
    }
  }

  def assertValidDefault(pd: Datatype, value: String) {
    validate(pd, value) match {
      case None => ()
      case Some(msg) => sys.error(msg)
    }
  }

  def validate(
    pd: Datatype,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    pd match {
      case Datatype.List(t) => {
        parseJsonOrNone(value) match {
          case None => {
            Some(s"default[$value] is not valid json")
          }
          case Some(json) => {
            json.asOpt[JsArray] match {
              case Some(v) => {
                Some(
                  v.value.flatMap { value =>
                    validate(t, JsonUtil.asOptString(value).getOrElse(""), errorPrefix)
                  }.mkString(", ")
                )
              }
              case None => {
                Some(s"default[$value] is not a valid JSON Array")
              }
            }
          }
        }
      }
      case Datatype.Map(t) => {
        parseJsonOrNone(value) match {
          case None => {
            Some(s"default[$value] is not valid json")
          }
          case Some(json) => {
            json.asOpt[JsObject] match {
              case Some(v) => {
                Some(
                  v.value.flatMap {
                    case (key, value) => {
                      validate(t, JsonUtil.asOptString(value).getOrElse(""), errorPrefix)
                    }
                  }.mkString(", ")
                )
              }
              case None => {
                Some(s"default[$value] is not a valid JSON Object")
              }
            }
          }
        }
      }
      case Datatype.Option(t) => {
        validateTypes(Seq(Type(TypeKind.Primitive, Primitives.Unit.toString), t), value, errorPrefix)
      }
      case Datatype.Singleton(t) => {
        validate(t, value, errorPrefix)
      }
      case Datatype.Union(types) => {
        validateTypes(types, value, errorPrefix)
      }
    }
  }

  private def validateTypes(
    types: Seq[Type],
    value: String,
    errorPrefix: Option[String]
  ): Option[String] = {
    types.map(t => validate(t, value, errorPrefix)).flatten match {
      case Nil => None
      case firstError :: errors => Some(firstError)
    }
  }


  def validate(
    t: Type,
    value: String,
    errorPrefix: Option[String]
  ): Option[String] = {
    t match {

      case Type(TypeKind.Enum, name) => {
        enums.find(_.name == name) match {
          case None => Some(s"could not find enum named[$name]")
          case Some(enum) => {
            enum.values.find(_ == value) match {
              case None => {
                Some(
                  withPrefix(
                    errorPrefix,
                    s"default[$value] is not a valid value for enum[$name]. Valid values are: " + enum.values.mkString(", ")
                  )
                )
              }
              case Some(_) => None
            }
          }
        }
      }
      
      case Type(TypeKind.Model, name) => {
        Some(withPrefix(errorPrefix, s"default[$value] is not valid for model[$name]. apidoc does not support default values for models"))
      }

      case Type(TypeKind.Primitive, name) => {
        Primitives(name) match {
          case None => {
            Some(withPrefix(errorPrefix, s"there is no primitive datatype[$name]"))
          }

          case Some(pt) => {
            pt match {
              case Primitives.Boolean => {
                if (PrimitiveMetadata.BooleanValues.contains(value)) {
                  None
                } else {
                  Some(withPrefix(errorPrefix, s"Value[$value] is not a valid boolean. Must be one of: ${PrimitiveMetadata.BooleanValues.mkString(", ")}"))
                }
              }

              case Primitives.Double => {
                Try(value.toDouble) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid double"))
                }
              }

              case Primitives.Integer => {
                Try(value.toInt) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid integer"))
                }
              }

              case Primitives.Long => {
                Try(value.toLong) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid long"))
                }
              }

              case Primitives.Object => {
                Try(Json.parse(value)) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid object"))
                }
              }

              case Primitives.Decimal => {
                Try(BigDecimal(value)) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid decimal"))
                }
              }

              case Primitives.Unit => {
                if (value == "") {
                  None
                } else {
                  Some(withPrefix(errorPrefix, s"Value[$value] is not a valid unit type - must be the empty string"))
                }
              }

              case Primitives.Uuid => {
                Try(UUID.fromString(value)) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid uuid"))
                }
              }

              case Primitives.DateIso8601 => {
                Try(dateTimeISOParser.parseDateTime(s"${value}T00:00:00Z")) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid date-iso8601"))
                }
              }

              case Primitives.DateTimeIso8601 => {
                Try(dateTimeISOParser.parseDateTime(value)) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid date-time-iso8601"))
                }
              }

              case Primitives.String => {
                None
              }
            }
          }
        }
      }
    }
  }

  private def withPrefix(
    prefix: Option[String],
    msg: String
  ) = prefix match {
    case None => msg
    case Some(p) => s"$p $msg"
  }

}
