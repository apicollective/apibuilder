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

sealed trait TypeInstance

object TypeInstance {

  case class Singleton(datatype: Type) extends TypeInstance { override def toString = s"$datatype" }
  case class Optional(datatype: Type) extends TypeInstance { override def toString = s"option[$datatype]" }
  case class List(datatype: Type) extends TypeInstance { override def toString = s"list[$datatype]" }
  case class Map(datatype: Type) extends TypeInstance { override def toString = s"map[$datatype]" }

  private val ImplicitListRx = "^\\[(.*)\\]$".r
  private val ExplicitListRx = "^\\list[(.*)\\]$".r
  private val MapRx = "^\\map[(.*)\\]$".r
  private val OptionalRx = "^\\option[(.*)\\]$".r

  def apply(value: String): Option[TypeInstance] = {
    value match {
      case ImplicitListRx(name) => Type(name).map(TypeInstance.List(_))
      case ExplicitListRx(name) => Type(name).map(TypeInstance.List(_))
      case MapRx(name) => Type(name).map(TypeInstance.Map(_))
      case OptionalRx(name) => Type(name).map(TypeInstance.Optional(_))
      case _ => Type(value).map(TypeInstance.Singleton(_))
    }
  }

}
