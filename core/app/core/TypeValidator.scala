package core

import _root_.builder.JsonUtil
import io.apibuilder.spec.v0.models.Service
import lib.{DatatypeResolver, Kind, PrimitiveMetadata, Primitives}

import java.util.UUID
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}
import com.fasterxml.jackson.databind.JsonMappingException

import scala.util.{Failure, Success, Try}

trait TypesProviderWithName {

  def namespace: String
  def packageName: String
  def name: String
  def plural: String

  def fullName: String = {
    if (name.startsWith(namespace)) {
      name
    } else {
      Seq(namespace, packageName, name).mkString(".")
    }
  }

}

case class TypesProviderEnum(
  namespace: String,
  name: String,
  plural: String,
  values: Seq[String]
) extends TypesProviderWithName {

  override def packageName = "enums"

}

case class TypesProviderUnion(
  namespace: String,
  name: String,
  plural: String,
  types: Seq[TypesProviderUnionType]
) extends TypesProviderWithName {

  override def packageName = "unions"

}

case class TypesProviderUnionType(
  `type`: String
)

case class TypesProviderInterface(
  namespace: String,
  name: String,
  plural: String,
  fields: Seq[TypesProviderField],
) extends TypesProviderWithName {

  override def packageName = "interfaces"

}

case class TypesProviderModel(
  namespace: String,
  name: String,
  plural: String,
  fields: Seq[TypesProviderField]
) extends TypesProviderWithName {

  override def packageName = "models"

}

case class TypesProviderField(
  name: String,
  `type`: String
)

trait TypesProvider {

  def enums: Iterable[TypesProviderEnum]
  def interfaces: Iterable[TypesProviderInterface]
  def models: Iterable[TypesProviderModel]
  def unions: Iterable[TypesProviderUnion]

  def resolver: DatatypeResolver = DatatypeResolver(
    enumNames = enums.map(_.name),
    interfaceNames = interfaces.map(_.name),
    modelNames = models.map(_.name),
    unionNames = unions.map(_.name)
  )

}

object TypesProvider {

  case class FromService(service: Service) extends TypesProvider {

    override def enums: Iterable[TypesProviderEnum] = service.enums.map { enum =>
      TypesProviderEnum(
        namespace = service.namespace,
        name = enum.name,
        plural = enum.plural,
        values = enum.values.map(_.name)
      )
    }

    override def unions: Iterable[TypesProviderUnion] = service.unions.map { union =>
      TypesProviderUnion(
        namespace = service.namespace,
        name = union.name,
        plural = union.plural,
        types = union.types.map(_.`type`).map(TypesProviderUnionType(_))
      )
    }

    override def models: Iterable[TypesProviderModel] = service.models.map { model =>
      TypesProviderModel(
        namespace = service.namespace,
        name = model.name,
        plural = model.plural,
        fields = model.fields.map { f =>
          TypesProviderField(
            name = f.name,
            `type` = f.`type`
          )
        }
      )
    }

    override def interfaces: Iterable[TypesProviderInterface] = service.interfaces.map { interface =>
      TypesProviderInterface(
        namespace = service.namespace,
        name = interface.name,
        plural = interface.plural,
        fields = interface.fields.map { f =>
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

  private val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()

  private def parseJsonOrNone(value: String): Option[JsValue] = {
    Try(Json.parse(value)) match {
      case Success(jsValue) => Some(jsValue)
      case Failure(ex) => ex match {
        case _: JsonParseException => None
        case _: JsonMappingException => None
        case _: JsonProcessingException => None
        case e: Throwable => throw e
      }
    }
  }

  def validate(
    kind: Kind,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    kind match {
      case Kind.List(t) => {
        parseJsonOrNone(value) match {
          case None => {
            Some(withPrefix(errorPrefix, s"default[$value] is not valid json"))
          }
          case Some(json) => {
            json.asOpt[JsArray] match {
              case Some(v) => {
                v.value.flatMap { value =>
                  validate(t, JsonUtil.asOptString(value).getOrElse(""), errorPrefix)
                }.toList match {
                  case Nil => None
                  case errors => Some(errors.mkString(", "))
                }
              }
              case None => {
                Some(withPrefix(errorPrefix, s"default[$value] is not a valid JSON Array"))
              }
            }
          }
        }
      }
      case Kind.Map(t) => {
        parseJsonOrNone(value) match {
          case None => {
            Some(withPrefix(errorPrefix, s"default[$value] is not valid json"))
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
                Some(withPrefix(errorPrefix, s"default[$value] is not a valid JSON Object"))
              }
            }
          }
        }
      }

      case Kind.Enum(name) => {
        val names = defaultNamespace match {
          case None => Seq(name)
          case Some(ns) => Seq(name, s"$ns.enums.$name")
        }

        enums.find(e => names.contains(e.name)) match {
          case None => {
            // This occurs if the enum itself has been imported. At the moment,
            // we can validate the default values for imported enums.
            None
          }

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

      case Kind.Interface(name) => {
        Some(withPrefix(errorPrefix, s"default[$value] is not valid for interface[$name]. API Builder does not support default values for interfaces"))
      }

      case Kind.Model(name) => {
        Some(withPrefix(errorPrefix, s"default[$value] is not valid for model[$name]. API Builder does not support default values for models"))
      }

      case Kind.Union(name) => {
        Some(withPrefix(errorPrefix, s"default[$value] is not valid for union[$name]. API Builder does not support default values for unions"))
      }

      case Kind.Primitive(name) => {
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
                  case Success(_) => None
                  case Failure(_) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid double"))
                }
              }

              case Primitives.Integer => {
                Try(value.toInt) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid integer"))
                }
              }

              case Primitives.JsonValue => {
                Try(Json.parse(value)) match {
                  case Success(v) => None
                  case Failure(v) => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid json value"))
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
                  case Success(v: JsObject) => None
                  case _ => Some(withPrefix(errorPrefix, s"Value[$value] is not a valid object"))
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
