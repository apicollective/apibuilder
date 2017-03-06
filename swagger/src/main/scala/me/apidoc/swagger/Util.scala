package me.apidoc.swagger

import scala.collection.JavaConversions._
import collection.JavaConverters._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.io.File
import java.util.UUID

import io.swagger.{models => swaggermodels}
import io.swagger.models.{parameters => swaggerparams, properties => swaggerproperties}

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

  /**
    * Checks if the swagger parameter is a query or path parameter and if it has string enum values.
    */
  def hasStringEnum(param: swaggerparams.Parameter): Boolean = {
    (param.isInstanceOf[swaggerparams.PathParameter] || param.isInstanceOf[swaggerparams.QueryParameter]) && {
      param match {
        case p: swaggerparams.AbstractSerializableParameter[_] =>
          val enumerableParam = param.asInstanceOf[swaggerparams.AbstractSerializableParameter[_]]
          enumerableParam.getType.equals(swaggerproperties.StringProperty.TYPE) && enumerableParam.getEnum != null && !enumerableParam.getEnum.isEmpty
        case _ => false
      }
    }
  }

  /**
    * Checks if the swagger string property and has string enum values.
    */
  def hasStringEnum(stringProperty: swaggerproperties.StringProperty): Boolean = {
      stringProperty.getEnum!=null && !stringProperty.getEnum.isEmpty
  }

  def buildPropertyEnumTypeName(modelName: String, enumName: String) = {
    List(modelName, enumName).map(_.toLowerCase.capitalize).mkString //camel case with first letter capitalized
  }

  def buildParamEnumTypeName(resourceName: String, param: swaggerparams.Parameter, method: String): String = {
    param match {
      case p: swaggerparams.QueryParameter =>
        List(resourceName, p.getName, method, "Query").map(_.toLowerCase.capitalize).mkString //camel case with first letter capitalized
      case p: swaggerparams.PathParameter =>
        List(resourceName, p.getName, method, "Path").map(_.toLowerCase.capitalize).mkString //camel case with first letter capitalized
      case _ =>
        sys.error(s"Enumerations not supported for swagger parameters of type ${param.getClass}")
    }
  }

  def retrieveMethod(operation: swaggermodels.Operation, path: swaggermodels.Path): Option[swaggermodels.HttpMethod] =
    path.getOperationMap().find(_._2 == operation).map(_._1)

}
