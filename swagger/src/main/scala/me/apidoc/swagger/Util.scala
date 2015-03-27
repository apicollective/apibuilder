package me.apidoc.swagger

import lib.Text
import scala.collection.JavaConverters._

object Util {

  /**
    * Normalize names
    */
  def formatName(name: String): String = {
    name.trim
  }

  /**
    * Returns a if present; otherwise b
    */
  def choose[T](a: Option[T], b: Option[T]): Option[T] = {
    a match {
      case None => b
      case Some(_) => a
    }
  }

  /**
    * Turns null and empty strings into None.
    */
  def toOption(value: String): Option[String] = {
    if (value == null || value.trim.isEmpty) {
      None
    } else {
      Some(value.trim)
    }
  }

  /**
    * Turns null into empty map
    */
  def toMap[T](values: java.util.Map[String, T]): Map[String, T] = {
    if (values == null) {
      Map()
    } else {
      values.asScala.toMap
    }
  }

  /**
    * Turns null into empty list
    */
  def toArray[T](values: java.util.List[T]): Seq[T] = {
    if (values == null) {
      Nil
    } else {
      values.asScala
    }
  }

}
