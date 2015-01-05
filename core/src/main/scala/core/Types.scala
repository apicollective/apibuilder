package core

import com.gilt.apidocspec.models.Service
import lib.{Datatype, DatatypeResolver, Type}

private[core] case class TypesProviderEnum(
  name: String,
  values: Seq[String]
)

private[core] sealed trait TypesProvider {
  def enums: Iterable[TypesProviderEnum] = Seq.empty
  def modelNames: Iterable[String] = Seq.empty
}

private[core] case class ServiceTypesProvider(
  service: Service
) extends TypesProvider {

  private def qualifiedName(prefix: String, name: String): String = {
    s"${service.namespace}.$prefix.$name"
  }

  override def enums: Iterable[TypesProviderEnum] = service.enums.map { enum =>
    TypesProviderEnum(
      name = qualifiedName("enums", enum.name),
      values = enum.values.map(_.name)
    )
  }

  override def modelNames: Iterable[String] = service.models.map(n => qualifiedName("models", n.name))

}

private[core] case class InternalServiceFormTypesProvider(
  internal: InternalServiceForm
) extends TypesProvider {

  private val internalEnums: Seq[TypesProviderEnum] = internal.enums.map { enum =>
    TypesProviderEnum(
      name = enum.name,
      values = enum.values.flatMap(_.name)
    )
  }

  private val internaModelNames: Seq[String] = internal.models.map(_.name)

  private val imports: Seq[Import] = internal.imports.flatMap(_.uri).map(Import(_))
  private val importedServices: Seq[ServiceTypesProvider] = imports.map { imp =>
    ServiceTypesProvider(imp.service)
  }

  override def enums = internalEnums ++ importedServices.map(_.enums).flatten

  override def modelNames = internaModelNames ++ importedServices.map(_.modelNames).flatten

}

private[core] case class TypeResolver(
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
