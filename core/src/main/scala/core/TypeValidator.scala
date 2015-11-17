package core

import com.bryzek.apidoc.spec.v0.models.{Model, Service}
import builder.JsonUtil
import lib.{Datatype, PrimitiveMetadata, Primitives, Type, Kind}
import java.util.UUID
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException
import scala.util.{Failure, Success, Try}

case class TypesProviderEnum(
  name: String,
  plural: String,
  values: Seq[String]
)

case class TypesProviderUnion(
  name: String,
  plural: String,
  types: Seq[TypesProviderUnionType]
)

case class TypesProviderUnionType(
  `type`: String
)

case class TypesProviderModel(
  name: String,
  plural: String,
  fields: Seq[TypesProviderField]
)

case class TypesProviderField(
  name: String,
  `type`: String
)

trait TypesProvider {

  def enums: Iterable[TypesProviderEnum]

  def models: Iterable[TypesProviderModel]
  def unions: Iterable[TypesProviderUnion]

  def modelNames: Iterable[String] = models.map(_.name)
  def unionNames: Iterable[String] = unions.map(_.name)
}

object TypesProvider {

  case class FromService(service: Service) extends TypesProvider {

    // TODO: Figure out imports

    private[this] def qualifiedName(prefix: String, name: String): String = {
      s"${service.namespace}.$prefix.$name"
    }

    override def enums: Iterable[TypesProviderEnum] = service.enums.map { enum =>
      TypesProviderEnum(
        name = qualifiedName("enums", enum.name),
        plural = qualifiedName("enums", enum.plural),
        values = enum.values.map(_.name)
      )
    }

    override def unions: Iterable[TypesProviderUnion] = service.unions.map { union =>
      TypesProviderUnion(
        name = qualifiedName("unions", union.name),
        plural = qualifiedName("unions", union.plural),
        types = union.types.map(_.`type`).map(TypesProviderUnionType(_))
      )
    }

    override def models: Iterable[TypesProviderModel] = service.models.map { model =>
      TypesProviderModel(
        name = qualifiedName("models", model.name),
        plural = qualifiedName("models", model.plural),
        fields = model.fields.map { f =>
          TypesProviderField(
            name = f.name,
            `type` = f.`type`
          )
        }
      )
    }

  }

}

case class TypeValidator(
  defaultNamespace: Option[String],
  enums: Iterable[TypesProviderEnum] = Seq.empty
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
                v.value.flatMap { value =>
                  validate(t, JsonUtil.asOptString(value).getOrElse(""), errorPrefix)
                } match {
                  case Nil => None
                  case errors => Some(errors.mkString(", "))
                }
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
                v.value.flatMap {
                  case (key, value) => {
                    validate(t, JsonUtil.asOptString(value).getOrElse(""), errorPrefix)
                  }
                } match {
                  case Nil => None
                  case errors => Some(errors.mkString(", "))
                }
              }
              case None => {
                Some(s"default[$value] is not a valid JSON Object")
              }
            }
          }
        }
      }
      case Datatype.Singleton(t) => {
        validate(t, value, errorPrefix)
      }
    }
  }

  def validate(
    t: Type,
    value: String,
    errorPrefix: Option[String]
  ): Option[String] = {
    t match {

      case Type(Kind.Enum, name) => {
        val names = defaultNamespace match {
          case None => Seq(name)
          case Some(ns) => Seq(name, s"$ns.enums.$name")
        }

        enums.find(e => names.contains(e.name)) match {
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
      
      case Type(Kind.Model, name) => {
        Some(withPrefix(errorPrefix, s"default[$value] is not valid for model[$name]. apidoc does not support default values for models"))
      }

      case Type(Kind.Primitive, name) => {
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
