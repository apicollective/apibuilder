package core

import codegenerator.models.Enum

sealed trait Primitives

object Primitives {

  case object Boolean extends Primitives { override def toString = "boolean" }
  case object Decimal extends Primitives { override def toString = "decimal" }
  case object Integer extends Primitives { override def toString = "integer" }
  case object Double extends Primitives { override def toString = "double" }
  case object Long extends Primitives { override def toString = "long" }
  case object String extends Primitives { override def toString = "string" }
  case object DateIso8601 extends Primitives { override def toString = "date-iso8601" }
  case object DateTimeIso8601 extends Primitives { override def toString = "date-time-iso8601" }
  case object Uuid extends Primitives { override def toString = "uuid" }
  case object Unit extends Primitives { override def toString = "unit" }

  val All = Seq(Boolean, Decimal, Integer, Double, Long, String, DateIso8601, DateTimeIso8601, Uuid, Unit)

  def apply(value: String): Option[Primitives] = {
    All.find(_.toString == value.toLowerCase.trim)
  }

}

sealed trait Type

object Type {

  case class Primitive(primitive: Primitives) extends Type
  case class Model(name: String) extends Type
  case class Enum(name: String) extends Type

}

sealed trait TypeContainer

object TypeContainer {

  case object Singleton extends TypeContainer { override def toString = s"singleton" }
  case object List extends TypeContainer { override def toString = "list" }
  case object Map extends TypeContainer { override def toString = "map" }

}

case class TypeInstance(
  container: TypeContainer,
  `type`: Type
) {

  def assertValidDefault(enums: Seq[Enum], value: String) {
    TypeValidator(enums.map(e => TypeValidatorEnums(e.name, e.values.map(_.name)))).assertValidDefault(`type`, value)
  }

}

case class TypeResolver(
  enumNames: Seq[String] = Seq.empty,
  modelNames: Seq[String] = Seq.empty
) {

  def toType(name: String): Option[Type] = {
    Primitives(name) match {
      case Some(pt) => Some(Type.Primitive(pt))
      case None => {
        enumNames.find(_ == name) match {
          case Some(et) => Some(Type.Enum(name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(mt) => Some(Type.Model(name))
              case None => None
            }
          }
        }
      }
    }
  }

  def toTypeInstance(internal: InternalParsedDatatype): Option[TypeInstance] = {
    toType(internal.name).map { TypeInstance(internal.container, _) }
  }

}
