package core

import lib.{Datatype, DatatypeResolver, Type}

case class TypeResolver(
  enumNames: Iterable[String] = Seq.empty,
  modelNames: Iterable[String] = Seq.empty
) {

  private val resolver = DatatypeResolver(
    enumNames = enumNames,
    modelNames = modelNames
  )

  def toType(name: String): Option[Type] = {
    resolver.toType(name)
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
    resolver.parse(internal.label)
  }

}
