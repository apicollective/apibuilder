package core

import play.api.libs.json.{JsString, JsValue, JsUndefined}
import scala.util.{Failure, Success, Try}

/**
 * Parse numbers and string json values as strings
 */
private[core] object JsonUtil {

  def asOptString(value: JsValue): Option[String] = {
    value match {
      case (_: JsUndefined) => None
      case (v: JsString) => parseString(v.value)
      case (v: JsValue) => parseString(v.toString)
    }
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).flatMap { s =>
      if (s == "true") {
        Some(true)
      } else if (s == "false") {
        Some(false)
      } else {
        None
      }
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
