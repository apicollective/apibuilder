package core

import com.gilt.apidoc.spec.v0.models.{Import, Service}
import lib.{Datatype, DatatypeResolver, Type}
import scala.annotation.tailrec

private[core] case class TypesProviderEnum(
  name: String,
  values: Seq[String]
)

private[core] sealed trait TypesProvider {
  def enums: Iterable[TypesProviderEnum] = Seq.empty
  def modelNames: Iterable[String] = Seq.empty
}

private[core] object TypesProvider {

  case class ServiceProvider(service: Service) extends TypesProvider {

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

  case class InternalServiceFormProvider(internal: InternalServiceForm) extends TypesProvider {

    override def enums = internal.enums.map { enum =>
      TypesProviderEnum(
        name = enum.name,
        values = enum.values.flatMap(_.name)
      )
    }

    override def modelNames = internal.models.map(_.name)

  }
}

/**
  * Takes an internal service form and recursively builds up a type
  * provider for all enums and all models specified in the service or
  * in any of the imports. Takes care to avoid importing the same
  * service multiple times (based on uniqueness of the import URIs)
  */
private[core] case class RecursiveTypesProvider(
  internal: InternalServiceForm
) extends TypesProvider {

  override def enums = providers.map(_.enums).flatten

  override def modelNames = providers.map(_.modelNames).flatten

  private lazy val providers = Seq(TypesProvider.InternalServiceFormProvider(internal)) ++ resolve(internal.imports.flatMap(_.uri))

  private def resolve(
    importUris: Seq[String],
    imported: Set[String] = Set.empty
  ): Seq[TypesProvider] = {
    importUris.headOption match {
      case None => Seq.empty
      case Some(uri) => {
        if (imported.contains(uri.toLowerCase.trim)) {
          // already imported
          resolve(importUris.drop(1), imported)
        } else {
          val importer = Importer(uri)
          importer.validate match {
            case Nil => {
              Seq(TypesProvider.ServiceProvider(importer.service)) ++ resolve(importUris.drop(1), imported ++ Set(uri))
            }
            case errors => {
              // There are errors w/ this import - skip it
              resolve(importUris.drop(1), imported ++ Set(uri))
            }
          }
        }
      }
    }
  }

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
