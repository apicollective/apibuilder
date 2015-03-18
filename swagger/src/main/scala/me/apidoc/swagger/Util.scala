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

}
