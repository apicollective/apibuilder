package me.apidoc.swagger

import lib.Text
import scala.collection.JavaConverters._

object Util {

  def formatName(name: String): String = {
    name.trim
  }

  def toOption(value: String): Option[String] = {
    if (value == null || value.trim.isEmpty) {
      None
    } else {
      Some(value.trim)
    }
  }

  def toMap[T](values: java.util.Map[String, T]): Map[String, T] = {
    if (values == null) {
      Map()
    } else {
      values.asScala.toMap
    }
  }

}
