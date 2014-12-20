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

  def parseWithError(internal: InternalParsedDatatype): ParsedDatatype = {
    parse(internal).getOrElse {
      sys.error(s"Unrecognized datatype[${internal.label}]")
    }
  }

  /**
    * Resolves the type name into instances of a first class Type.
    */
  def parse(internal: InternalParsedDatatype): Option[ParsedDatatype] = {
    internal match {
      case InternalParsedDatatype.List(name) => {
        toType(name).map { n => ParsedDatatype.List(n) }
      }

      case InternalParsedDatatype.Map(name) => {
        toType(name).map { n => ParsedDatatype.Map(n) }
      }

      case InternalParsedDatatype.Option(name) => {
        toType(name).map { n => ParsedDatatype.Option(n) }
      }

      case InternalParsedDatatype.Singleton(name) => {
        toType(name).map { n => ParsedDatatype.Singleton(n) }
      }

      case InternalParsedDatatype.Union(names) => {
        val types = names.map { n => toType(n) }
        types.filter(!_.isDefined) match {
          case Nil => Some(ParsedDatatype.Union(types.flatten))
          case unknowns => None
        }
      }
    }
  }

}
