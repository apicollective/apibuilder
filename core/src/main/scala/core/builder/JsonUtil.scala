package builder

import play.api.libs.json.{Json, JsArray, JsBoolean, JsObject, JsString, JsValue, JsUndefined}
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
    prefix: Option[String] = None
  ): Seq[String] = {
    val keys = strings ++ optionalStrings ++ arraysOfObjects ++ optionalArraysOfObjects ++ optionalObjects ++ optionalBooleans

    val p = prefix match {
      case None => ""
      case Some(value) => s"$value "
    }

    val unrecognized = json.asOpt[JsObject] match {
      case None => Seq.empty
      case Some(v) => unrecognizedFieldsErrors(v, keys, prefix)
    }

    unrecognized ++
    strings.flatMap { field =>
      (json \ field) match {
        case o: JsString => None
        case u: JsUndefined => Some(s"${p}Missing $field")
        case _ => Some(s"${p}$field must be a string")
      }
    } ++
    optionalStrings.flatMap { field =>
      (json \ field) match {
        case o: JsString => None
        case u: JsUndefined => None
        case _ => Some(s"${p}$field, if present, must be a string")
      }
    } ++
    optionalBooleans.flatMap { field =>
      (json \ field) match {
        case o: JsBoolean => None
        case o: JsString => {
          parseBoolean(o.value) match {
            case None => Some(s"${p}$field, if present, must be a boolean or the string 'true' or 'false'")
            case Some(_) => None
          }
        }
        case u: JsUndefined => None
        case _ => Some(s"${p}$field, if present, must be a boolean")
      }
    } ++
    arraysOfObjects.flatMap { field =>
      (json \ field) match {
        case o: JsArray => validateArrayOfObjects(s"${p}elements of $field", o.value)
        case u: JsUndefined => Some(s"${p}Missing $field")
        case _ => Some(s"${p}$field must be an array")
      }
    } ++
    optionalArraysOfObjects.flatMap { field =>
      (json \ field) match {
        case o: JsArray => validateArrayOfObjects(s"${p}elements of $field", o.value)
        case u: JsUndefined => None
        case _ => Some(s"${p}$field, if present, must be an array")
      }
    } ++
    optionalObjects.flatMap { field =>
      (json \ field) match {
        case o: JsObject => None
        case u: JsUndefined => None
        case _ => Some(s"${p}$field, if present, must be an object")
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
      case one :: Nil => Seq((prefix.getOrElse("") + " ").trim + s"Unrecognized element[$one]")
      case multiple => Seq((prefix.getOrElse("") + " ").trim + s"Unrecognized elements[${multiple.sorted.mkString(", ")}]")
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
    asOptString(value).flatMap { s =>
      Try(s.toLong) match {
        case Success(v) => Some(v)
        case Failure(e) => None
      }
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

}
