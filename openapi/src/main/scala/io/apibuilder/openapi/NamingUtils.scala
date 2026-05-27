package io.apibuilder.openapi

import io.apibuilder.validation.ScalarType

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

case class NamingConfig(
  uniqueNames: Boolean = false,
  suffixLength: Int = 4,
)

object NamingUtils {

  val ApibuilderPrimitiveTypes: Set[String] = ScalarType.all.map(_.name).toSet

  def toSnakeCase(str: String): String =
    str.trim
      .replaceAll("[\\s\\-.]+", "_")
      .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
      .toLowerCase

  def uniqueSnakeCase(str: String, config: NamingConfig): String = {
    if (ApibuilderPrimitiveTypes.contains(str)) str
    else if (isArray(str)) arrayType(uniqueSnakeCase(extractFromArray(str), config))
    else if (isMap(str)) mapType(uniqueSnakeCase(extractFromMap(str), config))
    else if (config.uniqueNames) {
      val snake = toSnakeCase(str)
      s"${snake}_${hashString(snake).take(config.suffixLength)}"
    } else {
      toSnakeCase(str)
    }
  }

  def sanitizeEnumName(s: String): String =
    s.trim
      .replaceAll("\"", "")
      .replaceAll("\\s+", "_")

  def hashString(input: String): String = {
    val letters = 'a' to 'z'
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8))
    hashBytes.map { b => letters((b & 0xff) % letters.length) }.mkString
  }

  def arrayType(typeName: String): String = s"[$typeName]"
  def mapType(typeName: String): String = s"map[$typeName]"

  private def isArray(str: String): Boolean =
    str.startsWith("[") && str.endsWith("]")

  private def extractFromArray(str: String): String = {
    assert(isArray(str), s"$str is not an array (missing '[]')")
    str.drop(1).dropRight(1)
  }

  private def isMap(str: String): Boolean =
    str.startsWith("map[") && str.endsWith("]")

  private def extractFromMap(str: String): String = {
    assert(isMap(str), s"$str is not a map (missing 'map[]')")
    str.drop(4).dropRight(1)
  }
}
