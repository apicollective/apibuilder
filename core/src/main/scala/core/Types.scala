package core

import lib.{PrimitiveMetadata, Primitives}
import com.gilt.apidocgenerator.models._

case class TypeResolver(
  enumNames: Seq[String] = Seq.empty,
  modelNames: Seq[String] = Seq.empty
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
      case InternalDatatype.List(name) => {
        toType(name).map { n => Datatype.List(n) }
      }

      case InternalDatatype.Map(name) => {
        toType(name).map { n => Datatype.Map(n) }
      }

      case InternalDatatype.Option(name) => {
        toType(name).map { n => Datatype.Option(n) }
      }

      case InternalDatatype.Singleton(name) => {
        toType(name).map { n => Datatype.Singleton(n) }
      }

      case InternalDatatype.Union(names) => {
        val types = names.map { n => toType(n) }
        types.filter(!_.isDefined) match {
          case Nil => Some(Datatype.Union(types.flatten))
          case unknowns => None
        }
      }
    }
  }

}
