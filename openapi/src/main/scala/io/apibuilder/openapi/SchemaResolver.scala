package io.apibuilder.openapi

import io.apibuilder.validation.ScalarType
import sttp.apispec.{Schema, SchemaLike, SchemaType}

import scala.annotation.tailrec
import scala.collection.immutable.ListMap

object SchemaResolver {

  def refName(ref: String): String =
    ref.replaceAll("#/components/schemas/", "")

  def buildModelReferences(schemas: ListMap[String, SchemaLike]): Map[String, String] = {
    schemas.toSeq.flatMap {
      case (name, s: Schema) if s.$ref.isDefined =>
        Some(name -> refName(s.$ref.get))

      case (name, s: Schema) if s.allOf.nonEmpty && s.properties.isEmpty =>
        findFirstRef(s.allOf).map(name -> _)

      case (name, s: Schema) if hasType(s, SchemaType.String) && s.`enum`.isEmpty && s.properties.isEmpty =>
        Some(name -> ScalarType.StringType.name)

      case (name, s: Schema) =>
        detectMapType(s).map(name -> _)

      case _ => None
    }.toMap
  }

  def resolveReference(name: String, refs: Map[String, String]): String =
    resolveReference(name, refs, Set.empty)

  @tailrec
  private def resolveReference(name: String, refs: Map[String, String], seen: Set[String]): String = {
    if (seen.contains(name))
      sys.error(s"Cycle detected while resolving schema alias: ${seen.mkString(" -> ")} -> $name")
    else
      refs.get(name) match {
        case None => name
        case Some(target) => resolveReference(target, refs, seen + name)
      }
  }

  private[openapi] def mapValueType(ap: SchemaLike): String = ap match {
    case s: Schema if s.$ref.isDefined => refName(s.$ref.get)
    case s: Schema => SchemaConverter.simpleType(s).map(_.name).getOrElse(ScalarType.JsonType.name)
    case _ => ScalarType.JsonType.name
  }

  private[openapi] def detectMapType(s: Schema): Option[String] = {
    if (hasType(s, SchemaType.Object) && s.properties.isEmpty) {
      s.additionalProperties.map(ap => NamingUtils.mapType(mapValueType(ap)))
    } else {
      None
    }
  }

  private def findFirstRef(schemas: List[SchemaLike]): Option[String] = {
    schemas.collectFirst {
      case s: Schema if s.$ref.isDefined => refName(s.$ref.get)
    }
  }

  private[openapi] def hasType(schema: Schema, schemaType: SchemaType): Boolean =
    schema.`type`.exists(_.contains(schemaType))
}
