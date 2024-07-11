package io.apibuilder.swagger

import scala.jdk.CollectionConverters._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets
import java.io.File
import java.util.UUID

import com.fasterxml.jackson.databind.ObjectMapper
import io.apibuilder.spec.v0.models.Attribute
import io.swagger.{models => swaggermodels}
import io.swagger.models.{parameters => swaggerparams, properties => swaggerproperties}
import play.api.libs.json._

import scala.collection.immutable.ListMap

object Util {

  def writeToTempFile(contents: String): File = {
    val tmpPath = "/tmp/apidoc.swagger.tmp." + UUID.randomUUID.toString + ".json"
    writeToFile(tmpPath, contents)
    new File(tmpPath)
  }

  def writeToFile(path: String, contents: String): Unit = {
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
    values.flatten.filter(_.nonEmpty) match {
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
      values.asScala.to(ListMap)
    }
  }

  /**
    * Turns null into empty list
    */
  def toArray[T](values: java.util.List[T]): Seq[T] = {
    if (values == null) {
      Nil
    } else {
      values.asScala.toSeq
    }
  }

  def isEnum(model: swaggermodels.ModelImpl): Boolean = model.getEnum != null && !model.getEnum.isEmpty

  /**
    * Checks if the swagger parameter is a query or path parameter and if it has string enum values.
    */
  def hasStringEnum(param: swaggerparams.Parameter): Boolean = {
    (param.isInstanceOf[swaggerparams.PathParameter] || param.isInstanceOf[swaggerparams.QueryParameter]) && {
      param match {
        case enumerableParam: swaggerparams.AbstractSerializableParameter[?] =>
          enumerableParam.getType.equals(swaggerproperties.StringProperty.TYPE) &&
            enumerableParam.getEnum != null &&
            !enumerableParam.getEnum.isEmpty
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

  def buildPropertyEnumTypeName(modelName: String, enumName: String): String = {
    List(modelName, enumName).map(_.capitalize).mkString
  }

  def buildParamEnumTypeName(resourceName: String, param: swaggerparams.Parameter, method: String): String = {
    param match {
      case qp: swaggerparams.QueryParameter =>
        formatName(resourceName, qp.getName, method, "Query")
      case pp: swaggerparams.PathParameter =>
        formatName(resourceName, pp.getName, method, "Path")
      case _ =>
        sys.error(s"Enumerations not supported for swagger parameters of type ${param.getClass}")
    }
  }

  def formatName(resourceName: String, paramName: String, method: String, location: String): String = {
    List(
      resourceName,
      paramName.capitalize,
      method.toLowerCase.capitalize,
      location.capitalize).mkString
  }

  def retrieveMethod(operation: swaggermodels.Operation, path: swaggermodels.Path): Option[swaggermodels.HttpMethod] =
    path.getOperationMap.asScala.find(_._2 == operation).map(_._1)

  /*
   * Converts the AnyRef from a Swagger parsed document into a JsValue
   */
  def swaggerAnyToJsValue(value: Any): JsValue = value match {
    case v: Boolean => JsBoolean(v)
    case v: Double => JsNumber(BigDecimal(v))
    case v: Float => JsNumber(BigDecimal(v))
    case v: Int => JsNumber(BigDecimal(v))
    case v: java.util.Map[_, _] => {
      val m: Map[Any, Any] = v.asScala.toMap
      JsObject(
        m.keys.flatMap {
          case key: String => {
            val value = m(key)
            Some((key, swaggerAnyToJsValue(value)))
          }
          case _ => None
        }.toMap
      )
    }
    case v: java.util.List[_] => JsArray(v.asScala.map(swaggerAnyToJsValue))
    case v: String => JsString(v)
    case _ => JsNull // Would be ideal to warn here
  }

  /*
   * Useful to serialize (with Jackson) Java objects from Swagger parser
   */
  val JacksonSerializer = new ObjectMapper() //it's thread-safe
  def toJsonString(jsonJavaModel: java.lang.Object): String = JacksonSerializer.writeValueAsString(jsonJavaModel)

  /*
   * Transforms a Java JSON model into the corresponding PlayJson JsValue
   */
  def toJsValue(jsonJavaModel: java.lang.Object): JsValue = Json.parse(toJsonString(jsonJavaModel))

  /*
   * Transforms the result of getVendorExtensions into a list of Attributes
   */
  def vendorExtensionsToAttributes(extensions: java.util.Map[String, Object]): Seq[Attribute] = {
    vendorExtensionsToAttributesOpt(extensions).getOrElse(Seq.empty)
  }

  /*
   * Transforms the result of getVendorExtensions into an optional list of Attributes
   */
  def vendorExtensionsToAttributesOpt(extensions: java.util.Map[String, Object]): Option[Seq[Attribute]] = {
    if (Option(extensions).isEmpty || extensions.isEmpty) None
    else Some(
      extensions.asScala.flatMap { case (key, value) =>
        swaggerAnyToJsValue(value) match {
          case v: JsObject => Some(Attribute(key.replaceFirst("^x-", ""), v))
          case _ => None // Would be ideal to warn here
        }
      }.toSeq
    )
  }
}
