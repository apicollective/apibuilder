package models

import scala.io.Source
import java.io.File
import play.api.libs.json.{ Json, JsArray, JsValue, JsUndefined }

case class JsonParser(contents: String) {
  lazy val json = Json.parse(contents)

  def getValue(value: JsValue, fieldName: String): JsValue = {
    getOptionalValue(value, fieldName).getOrElse {
      sys.error(s"missing key named $fieldName")
    }
  }

  def getOptionalValue(value: JsValue, fieldName: String): Option[JsValue] = {
    (value \ fieldName) match {
      case _: JsUndefined => { None }
      case v: JsValue => { Some(v) }
    }
  }

  def getArray(value: JsValue, fieldName: String): Seq[JsValue] = {
    getOptionalValue(value, fieldName) match {
      case Some(v) => { v.as[JsArray].value }
      case None => { Seq.empty }
    }
  }

}

