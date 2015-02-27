package me.apidoc.avro

import org.apache.avro.Schema

sealed trait SchemaType

/**
  * Scala enumeration of the AVRO types
  */
object SchemaType {

  case object Array extends SchemaType
  case object Boolean extends SchemaType
  case object Bytes extends SchemaType
  case object Double extends SchemaType
  case object Enum extends SchemaType
  case object Fixed extends SchemaType
  case object Float extends SchemaType
  case object Int extends SchemaType
  case object Long extends SchemaType
  case object Map extends SchemaType
  case object Null extends SchemaType
  case object Record extends SchemaType
  case object String extends SchemaType
  case object Union extends SchemaType

  val all = Seq(Array, Boolean, Bytes, Double, Enum, Fixed, Float, Int, Long, Map, Null, Record, String, Union)

  private[this]
  val byName = all.map(x => x.toString.toLowerCase -> x).toMap

  def fromAvro(avroType: org.apache.avro.Schema.Type) = fromString(avroType.toString)

  def fromString(value: String): Option[SchemaType] = byName.get(value.toLowerCase)

}

