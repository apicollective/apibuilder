package me.apidoc.swagger

import lib.Text
import scala.collection.JavaConverters._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.io.File
import java.util.UUID

object Util {

  def writeToTempFile(contents: String): File = {
    val tmpPath = "/tmp/apidoc.swagger.tmp." + UUID.randomUUID.toString + ".json"
    writeToFile(tmpPath, contents)
    new File(tmpPath)
  }

  def writeToFile(path: String, contents: String) {
    val outputPath = Paths.get(path)
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    Files.write(outputPath, bytes)
  }

  /**
    * Normalize names
    */
  def formatName(name: String): String = {
    name.trim
  }

  private val PathParams = """\{(.+?)\}""".r

  /**
    * Replace swagger {...} syntax with leading :
    */
  def substitutePathParameters(url: String): String = {
    PathParams.replaceAllIn(url, m => ":" + m.group(1))
  }

  /**
    * Combine all non empty values into one string
    */
  def combine(
    values: Seq[Option[String]],
    connector: String = "\n\n"
  ): Option[String] = {
    values.flatten.filter(!_.isEmpty) match {
      case Nil => None
      case nonEmptyValues => Some(nonEmptyValues.mkString(connector))
    }
  }

  def normalizeUrl(value: String): String = {
    value.toLowerCase.trim.replaceAll("_", "-")
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
