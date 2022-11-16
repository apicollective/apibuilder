package lib

import scala.annotation.tailrec

sealed trait Kind

object Kind {

  case class Enum(name: String) extends Kind { override def toString: String = name }
  case class Interface(name: String) extends Kind { override def toString: String = name }
  case class Model(name: String) extends Kind { override def toString: String = name }
  case class Primitive(name: String) extends Kind { override def toString: String = name }
  case class Union(name: String) extends Kind { override def toString: String = name }

  case class List(kind: Kind) extends Kind { override def toString = s"[$kind]" }
  case class Map(kind: Kind) extends Kind { override def toString = s"map[$kind]" }

}

case class DatatypeResolver(
  enumNames: Iterable[String],
  interfaceNames: Iterable[String],
  unionNames: Iterable[String],
  modelNames: Iterable[String]
) {

  /**
   * Takes the name of a singleton type - a primitive, model or enum. If
   * valid - returns an instance of a Type. Types are resolved in the
   * following order:
   *
   *   1. Primitive
   *      2. Enum
   *      3. Model
   *      4. Union
   *
   * If the type is not found, returns none.
   *
   * Examples:
   * toSingletonType("string") => Some(Primitives.String)
   * toSingletonType("long") => Some(Primitives.Long)
   * toSingletonType("foo") => None
   */
  private[this] def toSingletonType(name: String): Seq[Kind] =
    Primitives(name) match {
      case Some(_) => {
        Seq(Kind.Primitive(name))
      }
      case None => {
        (enumNames.find(_ == name) match {
          case Some(_) => Seq(Kind.Enum(name))
          case None => {
            modelNames.find(_ == name) match {
              case Some(_) => Seq(Kind.Model(name))
              case None => {
                unionNames.find(_ == name) match {
                  case Some(_) => Seq(Kind.Union(name))
                  case None => Nil
                }
              }
            }
          }
        }) ++ interfaceNames.find(_ == name).map(Kind.Interface).toSeq
      }
    }

  /**
    * Parses a type string into an instance of a Datatype.
    *
    * @param value: Examples: "string", "uuid", "map[string]", "map[map[string]]"
    */
  def parse(value: String)(
    implicit acceptsKind: Kind => Boolean = {_ => true}
  ): Option[Kind] = {
    TextDatatype.parse(value).reverse match {
      case Nil => None
      case one :: rest => {
        one match {
          case TextDatatype.List => None
          case TextDatatype.Map => None
          case TextDatatype.Singleton(name) => {
            toSingletonType(name).find(acceptsKind) match {
              case None => None
              case Some(t) => {
                if (!isSingleton(t)) {
                  // Type must end in a concrete type, not a container
                  None
                } else {
                  rest.find { isSingleton } match {
                    case None => Some(parse(t, rest))
                    case Some(_) => None
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * Recursively parse the type, applying layers of containers (list
    * or map) until we have either fully parsed the type or have run
    * into an error. This can parse types like "map[[string]]" but
    * will throw errors if it finds a non container type.
    */
  @tailrec
  private[this] def parse(kind: Kind, rest: Seq[TextDatatype]): Kind = {
    rest.headOption match {
      case None => {
        kind
      }

      case Some(t) => {
        t match {
          case TextDatatype.List => parse(Kind.List(kind), rest.drop(1))
          case TextDatatype.Map => parse(Kind.Map(kind), rest.drop(1))
          case TextDatatype.Singleton(_) => sys.error("Singleton type found in non tail position")
        }
      }
    }
  }

  private[this] def isSingleton(kind: Kind): Boolean = {
    kind match {
      case Kind.Enum(_) | Kind.Model(_) | Kind.Interface(_) | Kind.Primitive(_) | Kind.Union(_) => true
      case Kind.List(_) | Kind.Map(_) => false
    }
  }

  private[this] def isSingleton(td: TextDatatype): Boolean = {
    td match {
      case TextDatatype.Singleton(_) => true
      case TextDatatype.List | TextDatatype.Map => false
    }
  }

}
