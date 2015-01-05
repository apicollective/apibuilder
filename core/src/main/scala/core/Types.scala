package core

import lib.{Datatype, DatatypeResolver, Type}

case class TypesProviderEnum(
  name: String,
  values: Seq[String]
)

sealed trait TypesProvider {
  def enums: Iterable[TypesProviderEnum] = Seq.empty
  def modelNames: Iterable[String] = Seq.empty
}

case class InternalServiceFormTypesProvider(
  internal: InternalServiceForm
) extends TypesProvider {

  override def enums = {
    internal.enums.map { enum =>
      TypesProviderEnum(
        name = enum.name,
        values = enum.values.flatMap(_.name)
      )
    }
  }

  override def modelNames = internal.models.map(_.name)

}

case class TypeResolver(
  provider: TypesProvider
) {

  private val resolver = DatatypeResolver(
    enumNames = provider.enums.map(_.name),
    modelNames = provider.modelNames
  )

  private lazy val validator = TypeValidator(provider.enums)

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

  def assertValidDefault(pd: Datatype, value: String) {
    validate(pd, value) match {
      case None => {}
      case Some(msg) => sys.error(msg)
    }
  }

  def validate(
    pd: Datatype,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    validator.validate(pd, value, errorPrefix)
  }

}
