package core

import play.api.libs.json.{JsString, JsValue, JsUndefined}

/**
 * Parse numbers and string json values as strings
 */
private[core] object JsonUtil {

  def asOptString(json: JsValue, field: String): Option[String] = {
    val value = (json \ field)
    asOptString(value)
  }

  def asOptString(value: JsValue): Option[String] = {
    value match {
      case (_: JsUndefined) => None
      case (v: JsString) => parseString(v.value)
      case (v: JsValue) => parseString(v.toString)
    }
  }

  def asOptBoolean(value: JsValue): Option[Boolean] = {
    asOptString(value).map(_ == "true")
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
