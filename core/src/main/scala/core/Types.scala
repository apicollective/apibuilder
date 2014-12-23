package core

import lib.{Datatype, PrimitiveMetadata, Primitives, Type, TypeKind}

case class TypeResolver(
  enumNames: Iterable[String] = Seq.empty,
  modelNames: Iterable[String] = Seq.empty
) {

  /**
    * Takes the name of a simple type - a primitive, model or enum. If
    * valid - returns a Type/
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

  def parseWithError(internal: InternalDatatype): Datatype = {
    parse(internal).getOrElse {
      sys.error(s"Unrecognized datatype[${internal.label}]")
    }
  }

  /**
    * Resolves the type name into instances of a first class Type.
    */
  def parse(internal: InternalDatatype): Option[Datatype] = {
    internal match {
      case InternalDatatype.List(names) => {
        toTypesIfAllFound(names).map { Datatype.List(_) }
      }

      case InternalDatatype.Map(names) => {
        toTypesIfAllFound(names).map { Datatype.Map(_) }
      }

      case InternalDatatype.Option(names) => {
        toTypesIfAllFound(names).map { Datatype.Option(_) }
      }

      case InternalDatatype.Singleton(names) => {
        toTypesIfAllFound(names).map { Datatype.Singleton(_) }
      }
    }
  }

  private def toTypesIfAllFound(names: Seq[String]): Option[Seq[Type]] = {
    val types = names.map { n => toType(n) }
    types.filter(!_.isDefined) match {
      case Nil => Some(types.flatten)
      case unknowns => None
    }
  }

}
