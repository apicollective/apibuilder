package lib

sealed trait Primitives

object Primitives {

  case object Boolean extends Primitives { override def toString = "boolean" }
  case object Decimal extends Primitives { override def toString = "decimal" }
  case object Integer extends Primitives { override def toString = "integer" }
  case object Double extends Primitives { override def toString = "double" }
  case object Long extends Primitives { override def toString = "long" }
  case object Object extends Primitives { override def toString = "object" }
  case object String extends Primitives { override def toString = "string" }
  case object DateIso8601 extends Primitives { override def toString = "date-iso8601" }
  case object DateTimeIso8601 extends Primitives { override def toString = "date-time-iso8601" }
  case object Uuid extends Primitives { override def toString = "uuid" }
  case object Unit extends Primitives { override def toString = "unit" }

  val All = Seq(Boolean, Decimal, Integer, Double, Long, Object, String, DateIso8601, DateTimeIso8601, Uuid, Unit)

  val ValidInPath = All.filter(p => p != Unit && p != Object)

  def validInPath(name: String): Boolean = {
    ValidInPath.find(_.toString == name) match {
      case None => false
      case Some(_) => true
    }
  }

  def apply(value: String): Option[Primitives] = {
    All.find(_.toString == value.toLowerCase.trim)
  }

}

