package lib

sealed trait Datatype {
  def `type`: Type
  def label: String
}

object Datatype {

  case class List(`type`: Type) extends Datatype {
    override def label = "[" + `type`.name + "]"
  }

  case class Map(`type`: Type) extends Datatype {
    override def label = "map[" + `type`.name + "]"
  }

  case class Singleton(`type`: Type) extends Datatype {
    override def label = `type`.name
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
  case object Union extends Kind { override def toString = "union" }
  case object Enum extends Kind { override def toString = "enum" }

}

case class DatatypeResolver(
  enumNames: Iterable[String],
  unionNames: Iterable[String],
  modelNames: Iterable[String]
) {

  /**
    * Takes the name of a singleton type - a primitive, model or enum. If
    * valid - returns an instance of a Type. Types are resolved in the
    * following order:
    * 
    *   1. Primitive
    *   2. Enum
    *   3. Model
    *   4. Union
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
      case Some(_) => {
        Some(Type(Kind.Primitive, name))
      }
      case None => {
        enumNames.find(_ == name) match {
          case Some(_) => Some(Type(Kind.Enum, name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(_) => Some(Type(Kind.Model, name))
              case None => {
                unionNames.find(_ == name) match {
                  case Some(_) => Some(Type(Kind.Union, name))
                  case None => None
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * Parses a type string into an instance of a Datatype.
    * 
    * @param value: Examples: "string", "uuid"
    */
  def parse(value: String): Option[Datatype] = {
    TextDatatype(value) match {
      case TextDatatype.List(typeName) => toType(typeName).map { Datatype.List(_) }
      case TextDatatype.Map(typeName) => toType(typeName).map { Datatype.Map(_) }
      case TextDatatype.Singleton(typeName) => toType(typeName).map { Datatype.Singleton(_) }
    }
  }

}
