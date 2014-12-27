package lib

sealed trait Datatype {
  def types: Seq[Type]
  def label: String

  assert(!types.isEmpty, "Datatype requires at least 1 type")
}

object Datatype {

  private val Divider = " | "

  case class List(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("[", Divider, "]")
  }

  case class Map(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("map[", Divider, "]")
  }

  case class Option(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString("option[", Divider, "]")
  }

  case class Singleton(types: Seq[Type]) extends Datatype {
    override def label = types.map(_.name).mkString(Divider)
  }

}

case class Type(
  typeKind: TypeKind,
  name: String
)

sealed trait TypeKind

object TypeKind {

  case object Primitive extends TypeKind { override def toString = "primitive" }
  case object Model extends TypeKind { override def toString = "model" }
  case object Enum extends TypeKind { override def toString = "enum" }

}

case class DatatypeResolver(
  enumNames: Iterable[String] = Seq.empty,
  modelNames: Iterable[String] = Seq.empty
) {

  /**
    * Takes the name of a singleton type - a primitive, model or enum. If
    * valid - returns an instance of a Type. Types are resolved in the
    * following order:
    * 
    *   1. Primitive
    *   2. Enum
    *   3. Model
    * 
    * If the type is not found, returns none.
    * 
    * Examples:
    *   toType("string") => Some(Primitives.String)
    *   toType("long") => Some(Primitives.Long)
    *   toType("foo") => None
    */
  def toType(name: String): Option[Type] = {
    Primitives(name) match {
      case Some(pt) => Some(Type(TypeKind.Primitive, name))
      case None => {
        enumNames.find(_ == name) match {
          case Some(et) => Some(Type(TypeKind.Enum, name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(mt) => Some(Type(TypeKind.Model, name))
              case None => None
            }
          }
        }
      }
    }
  }

  private val ListRx = "^\\[(.*)\\]$".r
  private val MapRx = "^map\\[(.*)\\]$".r
  private val MapDefaultRx = "^map$".r
  private val OptionRx = "^option\\[(.*)\\]$".r

  /**
    * Parses a type string into an instance of a Datatype.
    * 
    * @param value: Examples: "string", "string | uuid", "map[long]", "option[string | uuid]"
    */
  def parse(value: String): Option[Datatype] = {
    value match {
      case ListRx(names) => toTypesIfAllFound(names).map { Datatype.List(_) }
      case MapRx(names) => toTypesIfAllFound(names).map { Datatype.Map(_) }
      case MapDefaultRx() => toTypesIfAllFound(Primitives.String.toString).map { Datatype.Map(_) }
      case OptionRx(names) => toTypesIfAllFound(names).map { Datatype.Option(_) }
      case _ => toTypesIfAllFound(value).map { Datatype.Singleton(_) }
    }
  }

  private def parseTypeNames(value: String): Seq[String] = {
    value.split("\\|").map(_.trim)
  }

  private def toTypesIfAllFound(value: String): Option[Seq[Type]] = {
    val types = parseTypeNames(value).map { n => toType(n) }
    types.filter(!_.isDefined) match {
      case Nil => Some(types.flatten)
      case unknowns => None
    }
  }

}
