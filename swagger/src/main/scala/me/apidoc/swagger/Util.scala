package me.apidoc.swagger

import lib.Text

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

  def toMap[T](values: java.util.Map[String, T]): java.util.Map[String, T] = {
    if (values == null) {
      java.util.Collections.emptyMap[String, T]()
    } else {
      values
    }
  }

}
