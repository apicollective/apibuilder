package builder

import play.api.libs.json.{Json, JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, JsUndefined}
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
    optionalBooleans: Seq[String] = Nil,
    optionalNumbers: Seq[String] = Nil,
    optionalAnys: Seq[String] = Nil,
    prefix: Option[String] = None
  ): Seq[String] = {
    val keys = strings ++ optionalStrings ++ arraysOfObjects ++ optionalArraysOfObjects ++ optionalObjects ++ optionalBooleans ++ optionalNumbers ++ optionalAnys

    val unrecognized = json.asOpt[JsObject] match {
      case None => Seq.empty
      case Some(v) => unrecognizedFieldsErrors(v, keys, prefix)
    }

    unrecognized ++
    strings.flatMap { field =>
      (json \ field) match {
        case o: JsString => {
          parseString(o.value) match {
            case None => Some(withPrefix(prefix, s"$field must be a non empty string"))
            case Some(_) => None
          }
        }
        case u: JsUndefined => Some(withPrefix(prefix, s"Missing $field"))
        case _ => Some(withPrefix(prefix, s"$field must be a string"))
      }
    } ++
    optionalStrings.flatMap { field =>
      (json \ field) match {
        case o: JsString => None
        case u: JsUndefined => None
        case _ => Some(withPrefix(prefix, s"$field, if present, must be a string"))
      }
    } ++
    optionalBooleans.flatMap { field =>
      (json \ field) match {
        case o: JsBoolean => None
        case o: JsString => {
          parseBoolean(o.value) match {
            case None => Some(withPrefix(prefix, s"$field, if present, must be a boolean or the string 'true' or 'false'"))
            case Some(_) => None
          }
        }
        case u: JsUndefined => None
        case _ => Some(withPrefix(prefix, s"$field, if present, must be a boolean"))
      }
    } ++
    optionalNumbers.flatMap { field =>
      (json \ field) match {
        case o: JsNumber => None
        case o: JsString => {
          parseLong(o.value) match {
            case None => Some(withPrefix(prefix, s"$field, if present, must be a number"))
            case Some(_) => None
          }
        }
        case u: JsUndefined => None
        case _ => Some(withPrefix(prefix, s"$field, if present, must be a number"))
      }
    } ++
    arraysOfObjects.flatMap { field =>
      (json \ field) match {
        case o: JsArray => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value)
        case u: JsUndefined => Some(withPrefix(prefix, s"Missing $field"))
        case _ => Some(withPrefix(prefix, s"$field must be an array"))
      }
    } ++
    optionalArraysOfObjects.flatMap { field =>
      (json \ field) match {
        case o: JsArray => validateArrayOfObjects(withPrefix(prefix, s"elements of $field"), o.value)
        case u: JsUndefined => None
        case _ => Some(withPrefix(prefix, s"$field, if present, must be an array"))
      }
    } ++
    optionalObjects.flatMap { field =>
      (json \ field) match {
        case o: JsObject => None
        case u: JsUndefined => None
        case _ => Some(withPrefix(prefix, s"$field, if present, must be an object"))
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
      case (_: JsUndefined) => None
      case (v: JsString) => parseString(v.value)
      case (v: JsValue) => parseString(v.toString)
    }
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).flatMap { parseBoolean(_) }
  }

  private def parseBoolean(value: String): Option[Boolean] = {
    if (value == "true") {
      Some(true)
    } else if (value == "false") {
      Some(false)
    } else {
      None
    }
  }

  def asOptLong(value: JsValue): Option[Long] = {
    asOptString(value).flatMap { parseLong(_) }
  }

  private def parseLong(value: String): Option[Long] = {
    Try(value.toLong) match {
      case Success(v) => Some(v)
      case Failure(e) => None
    }
  }

  def hasKey(json: JsValue, field: String): Boolean = {
    (json \ field) match {
      case (_: JsUndefined) => false
      case _ => true
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
