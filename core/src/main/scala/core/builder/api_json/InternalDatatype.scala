package builder.api_json

import builder.JsonUtil
import lib.Primitives
import play.api.libs.json._


sealed trait InternalDatatype {

  def name: String
  def required: Boolean
  def label: String

  protected def makeLabel(prefix: String = "", postfix: String = ""): String = {
    prefix + name + postfix
  }

}

object InternalDatatype {
  case class List(name: String, required: Boolean) extends InternalDatatype {
    override def label: String = makeLabel("[", "]")
  }

  case class Map(name: String, required: Boolean) extends InternalDatatype {
    override def label: String = makeLabel("map[", "]")
  }

  case class Singleton(name: String, required: Boolean) extends InternalDatatype {
    override def label: String = makeLabel()
  }


  val Unit: InternalDatatype = InternalDatatype.Singleton(
    name = Primitives.Unit.toString,
    required = true
  )
}

private[api_json] case class InternalDatatypeBuilder() {

  private[this] val dynamicEnums = scala.collection.mutable.ListBuffer[InternalEnumForm]()
  private[this] val dynamicModels = scala.collection.mutable.ListBuffer[InternalModelForm]()
  private[this] val dynamicUnions = scala.collection.mutable.ListBuffer[InternalUnionForm]()

  private[this] val EnumMarker = "enum"
  private[this] val ModelMarker = "model"
  private[this] val UnionMarker = "union"

  def enumForms: List[InternalEnumForm] = dynamicEnums.toList
  def modelForms: List[InternalModelForm] = dynamicModels.toList
  def unionForms: List[InternalUnionForm] = dynamicUnions.toList

  private val ListRx = "^\\[(.*)\\]$".r
  private val MapRx = "^map\\[(.*)\\]$".r
  private val DefaultMapRx = "^map$".r

  private[this] def apply(r: JsLookupResult): Either[Seq[String], InternalDatatype] = {
    JsonUtil.asOptJsValue(r) match {
      case None => Left(Seq("must be an object"))
      case Some(v) => apply(v)
    }
  }

  private[this] def apply(value: JsValue): Either[Seq[String], InternalDatatype] = {
    value.asOpt[String] match {
      case Some(v) => fromString(v)
      case None => value.asOpt[JsObject] match {
        case Some(v) => inlineType(v)
        case None => Left(Seq("must be a string or an object"))
      }
    }
  }

  private[this] def inlineType(value: JsObject): Either[Seq[String], InternalDatatype] = {
    JsonUtil.asOptString(value \ EnumMarker) match {
      case None => {
        JsonUtil.asOptString(value \ ModelMarker) match {
          case None => {
            JsonUtil.asOptString(value \ UnionMarker) match {
              case None => Left(Seq(s"must specify field '$EnumMarker', '$ModelMarker' or '$UnionMarker'"))
              case Some(unionName) => {
                fromString(unionName) match {
                  case Left(errors) => {
                    Left(errors)
                  }

                  case Right(dt) => {
                    dynamicUnions.append(
                      InternalUnionForm(this, dt.name, value - UnionMarker)
                    )
                    Right(dt)
                  }
                }

              }
            }
          }

          case Some(modelName) => {
            fromString(modelName) match {
              case Left(errors) => {
                Left(errors)
              }

              case Right(dt) => {
                dynamicModels.append(
                  InternalModelForm(this, dt.name, value - ModelMarker)
                )
                Right(dt)
              }
            }
          }
        }
      }

      case Some(enumName) => {
        fromString(enumName) match {
          case Left(errors) => {
            Left(errors)
          }

          case Right(dt) => {
            dynamicEnums.append(
              InternalEnumForm(dt.name, value - EnumMarker)
            )
            Right(dt)
          }
        }
      }
    }
  }

  def fromString(value: String): Either[Seq[String], InternalDatatype] = {
    Option(value.trim).filter(_.nonEmpty) match {
      case None => Left(Seq("type must be a non empty string"))
      case Some(v) => {
        Right(
          v match {
            case ListRx(name) => InternalDatatype.List(formatName(name), required = true)
            case MapRx(name) => InternalDatatype.Map(formatName(name), required = true)
            case DefaultMapRx() => InternalDatatype.Map(Primitives.String.toString, required = true)
            case _ => InternalDatatype.Singleton(formatName(value), required = true)
          }
        )
      }
    }
  }

  /**
    * Make primitive datatype names case insensitive to user
    * input. e.g. accept both 'UUID' and 'uuid' as the uuid type.
    */
  private def formatName(name: String): String = {
    Primitives(name) match {
      case None => name
      case Some(p) => p.toString
    }
  }

  def parseTypeFromObject(json: JsObject): Either[Seq[String], InternalDatatype] = {
    apply(json \ "type") match {
      case Left(errors) => {
        Left(errors)
      }

      case Right(dt) => {
        Right(
          JsonUtil.asOptBoolean(json \ "required") match {
            case None => {
              dt
            }

            case Some(true) => {
              // User explicitly marked this required
              dt match {
                case InternalDatatype.List(name, _) => InternalDatatype.List(formatName(name), required = true)
                case InternalDatatype.Map(name, _) => InternalDatatype.Map(formatName(name), required = true)
                case InternalDatatype.Singleton(name, _) => InternalDatatype.Singleton(formatName(name), required = true)
              }
            }

            case Some(false) => {
              // User explicitly marked this optional
              dt match {
                case InternalDatatype.List(name, _) => InternalDatatype.List(formatName(name), required = false)
                case InternalDatatype.Map(name, _) => InternalDatatype.Map(formatName(name), required = false)
                case InternalDatatype.Singleton(name, _) => InternalDatatype.Singleton(formatName(name), required = false)
              }
            }
          }
        )
      }
    }
  }

}
