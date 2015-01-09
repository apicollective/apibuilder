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
  typeKind: Kind,
  name: String
)

sealed trait Kind

object Kind {

  case object Primitive extends Kind { override def toString = "primitive" }
  case object Model extends Kind { override def toString = "model" }
  case object Enum extends Kind { override def toString = "enum" }

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
      case Some(pt) => Some(Type(Kind.Primitive, name))
      case None => {
        enumNames.find(_ == name) match {
          case Some(et) => Some(Type(Kind.Enum, name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(mt) => Some(Type(Kind.Model, name))
              case None => None
            }
          }
        }
      }
    }
  }

  /**
    * Parses a type string into an instance of a Datatype.
    * 
    * @param value: Examples: "string", "string | uuid", "map[long]", "option[string | uuid]"
    */
  def parse(value: String): Option[Datatype] = {
    TextDatatype(value) match {
      case TextDatatype.List(typeName) => toTypesIfAllFound(typeName).map { Datatype.List(_) }
      case TextDatatype.Map(typeName) => toTypesIfAllFound(typeName).map { Datatype.Map(_) }
      case TextDatatype.Option(typeName) => toTypesIfAllFound(typeName).map { Datatype.Option(_) }
      case TextDatatype.Singleton(typeName) => toTypesIfAllFound(typeName).map { Datatype.Singleton(_) }
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
