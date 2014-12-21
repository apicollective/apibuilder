package core

import lib.{PrimitiveMetadata, Primitives}
import com.gilt.apidocgenerator.models._

case class TypeResolver(
  enumNames: Seq[String] = Seq.empty,
  modelNames: Seq[String] = Seq.empty
) {

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

  def toTypeInstance(internal: InternalParsedDatatype): Option[TypeInstance] = {
    toType(internal.name).map { TypeInstance(internal.container, _) }
  }

}
