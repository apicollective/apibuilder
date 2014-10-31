package core

sealed trait Type

object Type {

  case object Boolean extends Type { override def toString = "boolean" }
  case object Decimal extends Type { override def toString = "decimal" }
  case object Integer extends Type { override def toString = "integer" }
  case object Double extends Type { override def toString = "double" }
  case object Long extends Type { override def toString = "long" }
  case object String extends Type { override def toString = "string" }
  case object DateIso8601 extends Type { override def toString = "date-iso8601" }
  case object DateTimeIso8601 extends Type { override def toString = "date-time-iso8601" }
  case object Uuid extends Type { override def toString = "uuid" }
  case object Unit extends Type { override def toString = "unit" }

  val All = Seq(Boolean, Decimal, Integer, Double, Long, String, DateIso8601, DateTimeIso8601, Uuid, Unit)

  def apply(value: String): Option[Type] = {
    All.find(_.toString == value.toLowerCase.trim)
  }

}

sealed trait TypeContainer

object TypeContainer {

  case object Singleton extends TypeContainer { override def toString = s"singleton" }
  case object List extends TypeContainer { override def toString = "list" }
  case object Map extends TypeContainer { override def toString = "map" }

}

case class TypeInstance(
  container: TypeContainer,
  datatype: Type
)
