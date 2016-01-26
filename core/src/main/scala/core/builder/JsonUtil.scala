package builder

import play.api.libs.json.{Json, JsArray, JsBoolean, JsLookupResult, JsNumber, JsObject, JsString, JsValue}
import scala.util.{Failure, Success, Try}

/**
 * Parse numbers and string json values as strings
 */
object JsonUtil {

  def validate(
    json: JsValue,
    strings: Seq[String] = Nil,
    optionalStrings: Seq[String] = Nil,
    arraysOfObjects: Seq[String] = Nil,
    optionalArraysOfObjects: Seq[String] = Nil,
    optionalObjects: Seq[String] = Nil,
    objects: Seq[String] = Nil,
    optionalBooleans: Seq[String] = Nil,
    optionalNumbers: Seq[String] = Nil,
    optionalAnys: Seq[String] = Nil,
    prefix: Option[String] = None
  ): Seq[String] = {
    val keys = strings ++ optionalStrings ++ arraysOfObjects ++ optionalArraysOfObjects ++ optionalObjects ++ objects ++ optionalBooleans ++ optionalNumbers ++ optionalAnys

    val unrecognized = json.asOpt[JsObject] match {
      case None => Seq.empty
      case Some(v) => unrecognizedFieldsErrors(v, keys, prefix)
    }

    unrecognized ++
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
    optionalStrings.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsString) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be a string"))
        case None => None
      }
    } ++
    optionalBooleans.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsBoolean) => None
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
        case Some(o: JsNumber) => None
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
    arraysOfObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsArray) => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value)
        case Some(_) => Some(withPrefix(prefix, s"$field must be an array"))
        case None => Some(withPrefix(prefix, s"Missing $field"))
      }
    } ++
    optionalArraysOfObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsArray) => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value)
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be an array"))
        case None => None
      }
    } ++
    optionalObjects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsObject) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, if present, must be an object"))
        case None => None
      }
    } ++
    objects.flatMap { field =>
      (json \ field).toOption match {
        case Some(o: JsObject) => None
        case Some(_) => Some(withPrefix(prefix, s"$field, must be an object"))
        case None => Some(withPrefix(prefix, s"Missing $field"))
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
          case o: JsObject => None
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
    val keys = json.value map { case (key, value) => key }

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
    value.toOption.flatMap { asOptString(_) }
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).flatMap { parseBoolean(_) }
  }

  def asOptBoolean(value: JsLookupResult): Option[Boolean] = {
    value.toOption.flatMap { asOptBoolean(_) }
  }

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
    asOptString(value).flatMap { parseLong(_) }
  }

  def asOptLong(value: JsLookupResult): Option[Long] = {
    value.toOption.flatMap { asOptLong(_) }
  }

  private def parseLong(value: String): Option[Long] = {
    Try(value.toLong) match {
      case Success(v) => Some(v)
      case Failure(e) => None
    }
  }

  def hasKey(json: JsValue, field: String): Boolean = {
    (json \ field).toOption match {
      case None => false
      case Some(_) => true
    }
  }

  private def parseString(value: String): Option[String] = {
    value.trim.isEmpty match {
      case true => None
      case false => Some(value.trim)
    }
  }

  private def withPrefix(prefix: Option[String], message: String): String = {
    prefix match {
      case None => message
      case Some(value) => s"$value $message"
    }
  }
}
