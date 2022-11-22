package builder

import cats.implicits._
import cats.data.ValidatedNec
import play.api.libs.json.{JsArray, JsBoolean, JsLookupResult, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.util.{Failure, Success, Try}

/**
 * Parse numbers and string json values as strings
 */
object JsonUtil {

  def validate(
    json: JsValue,
    strings: Seq[String] = Nil,
    optionalStrings: Seq[String] = Nil,
    anys: Seq[String] = Nil,
    arrayOfAnys: Seq[String] = Nil,
    arrayOfObjects: Seq[String] = Nil,
    optionalArraysOfStrings: Seq[String] = Nil,
    optionalArraysOfObjects: Seq[String] = Nil,
    optionalObjects: Seq[String] = Nil,
    objects: Seq[String] = Nil,
    optionalBooleans: Seq[String] = Nil,
    optionalNumbers: Seq[String] = Nil,
    optionalAnys: Seq[String] = Nil,
    prefix: Option[String] = None
  ): ValidatedNec[String, Unit] = {
    val keys = strings ++ anys ++ optionalStrings ++ arrayOfAnys ++ arrayOfObjects ++ optionalArraysOfStrings ++ optionalArraysOfObjects ++ optionalObjects ++ objects ++ optionalBooleans ++ optionalNumbers ++ optionalAnys

    val unrecognized = json.asOpt[JsObject] match {
      case None => Seq.empty
      case Some(v) => unrecognizedFieldsErrors(v, keys, prefix)
    }

    val allErrors = unrecognized ++
    strings.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsString) => {
          parseString(o.value) match {
            case None => Some(withPrefix(prefix, s"$field must be a non empty string"))
            case Some(_) => None
          }
        }
        case Some(_) => Some(withPrefix(prefix, s"$field must be a string"))
        case None => Some(withPrefix(prefix, s"Missing $field"))
      }
    } ++
      anys.flatMap { field =>
        (json \ field).toOption match {
          case Some(_) => None
          case None => Some(withPrefix(prefix, s"Missing $field"))
        }
      } ++
    optionalStrings.flatMap { field =>
      (json \ field).toOption match {
        case Some(_: JsString) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be a string"))
        case None => None
      }
    } ++
    optionalBooleans.flatMap { field =>
      (json \ field).toOption match {
        case Some(_: JsBoolean) => None
        case Some(o: JsString) => {
          parseBoolean(o.value) match {
            case None => Some(withPrefix(prefix, s"$field, if present, must be a boolean or the string 'true' or 'false'"))
            case Some(_) => None
          }
        }
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be a boolean"))
        case None => None
      }
    } ++
    optionalNumbers.flatMap { field =>
      (json \ field).toOption match {
        case Some(_: JsNumber) => None
        case Some(o: JsString) => {
          parseLong(o.value) match {
            case None => Some(withPrefix(prefix, s"$field, if present, must be a number"))
            case Some(_) => None
          }
        }
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be a number"))
        case None => None
      }
    } ++
    arrayOfAnys.flatMap { field =>
      (json \ field).toOption match {
        case Some(_) => None
        case None => Some(withPrefix(prefix, s"Missing $field"))
      }
    } ++
    arrayOfObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsArray) => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value.toSeq)
        case Some(_) => Some(withPrefix(prefix, s"$field must be an array"))
        case None => Some(withPrefix(prefix, s"Missing $field"))
      }
    } ++
    optionalArraysOfStrings.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsArray) => validateArrayOfStrings(withPrefix(prefix, s"elements of $field"), o.value.toSeq)
        case Some(_) => Some(withPrefix(prefix, s"$field must be an array"))
        case None => None
      }
    } ++
    optionalArraysOfObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsArray) => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value.toSeq)
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be an array"))
        case None => None
      }
    } ++
    optionalObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(_: JsObject) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be an object"))
        case None => None
      }
    } ++
    objects.flatMap { field =>
      (json \ field).toOption match {
        case Some(_: JsObject) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, must be an object"))
        case None => Some(withPrefix(prefix, s"Missing $field"))
      }
    }

    if (allErrors.isEmpty) {
      ().validNec
    } else {
      allErrors.distinct.map(_.invalidNec).sequence.map(_ => ())
    }
  }

  private def validateArrayOfStrings(
    prefix: String,
    js: Seq[JsValue]
  ): Option[String] = {
    js.headOption match {
      case None => None
      case Some(o) => {
        o match {
          case _: JsString => None
          case _ => Some(s"${prefix} must be strings")
        }
      }
    }
  }


  private def validateArrayOfObjects(
    prefix: String,
    js: Seq[JsValue]
  ): Option[String] = {
    js.headOption match {
      case None => None
      case Some(o) => {
        o match {
          case _: JsObject => None
          case _ => Some(s"${prefix} must be objects")
        }
      }
    }
  }

  private def unrecognizedFieldsErrors(
    json: JsObject,
    fields: Seq[String],
    prefix: Option[String] = None
  ): Seq[String] = {
    val keys = json.value map { case (key, _) => key }

    keys.filter { k => !fields.contains(k) }.toList match {
      case Nil => Nil
      case one :: Nil => Seq(withPrefix(prefix, s"Unrecognized element[$one]"))
      case multiple => Seq(withPrefix(prefix, s"Unrecognized elements[${multiple.sorted.mkString(", ")}]"))
    }
  }

  def asOptString(value: JsValue): Option[String] = {
    value match {
      case v: JsString => parseString(v.value)
      case v: JsValue => parseString(v.toString)
    }
  }

  def asOptString(value: JsLookupResult): Option[String] = {
    asOptJsValue(value).flatMap { asOptString }
  }

  def asOptJsValue(value: JsLookupResult): Option[JsValue] = {
    value.toOption
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).flatMap { parseBoolean }
  }

  def asOptBoolean(value: JsLookupResult): Option[Boolean] = {
    value.toOption.flatMap { asOptBoolean }
  }

  def asSeqOfString(value: JsValue): Seq[String] = {
    value match {
      case JsNull => Nil
      case a: JsArray => a.value.flatMap(v=> asOptString(v)).toSeq
      case JsString(text) => parseString(text).toSeq
      case v => parseString(v.toString()).toSeq
    }
  }

  def asSeqOfString(value: JsLookupResult): Seq[String] = asSeqOfString(value.getOrElse(JsNull))

  def parseBoolean(value: String): Option[Boolean] = {
    if (value == "true") {
      Some(true)
    } else if (value == "false") {
      Some(false)
    } else {
      None
    }
  }

  def isNumeric(value: String): Boolean = {
    Try(value.toLong) match {
      case Success(_) => true
      case Failure(_) => false
    }
  }

  def asOptLong(value: JsValue): Option[Long] = {
    asOptString(value).flatMap { parseLong }
  }

  def asOptLong(value: JsLookupResult): Option[Long] = {
    value.toOption.flatMap { asOptLong }
  }

  def parseBigDecimal(value: String): Option[BigDecimal] = {
    Try(BigDecimal(value)) match {
      case Success(v) => Some(v)
      case Failure(_) => None
    }
  }

  private def parseLong(value: String): Option[Long] = {
    Try(value.toLong) match {
      case Success(v) => Some(v)
      case Failure(_) => None
    }
  }

  def hasKey(json: JsValue, field: String): Boolean = {
    (json \ field).toOption match {
      case None => false
      case Some(_) => true
    }
  }

  private def parseString(value: String): Option[String] = {
    Some(value.trim).filter(_.nonEmpty)
  }

  private def withPrefix(prefix: Option[String], message: String): String = {
    prefix match {
      case None => message
      case Some(value) => s"$value $message"
    }
  }
}
