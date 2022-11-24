package builder.api_json

import cats.data.Validated.{Invalid, Valid}
import core.{Importer, TypeValidator, TypesProvider, TypesProviderEnum, TypesProviderField, TypesProviderInterface, TypesProviderModel, TypesProviderUnion}
import lib.{DatatypeResolver, Kind}

private[api_json] case class InternalServiceFormTypesProvider(internal: InternalServiceForm) extends TypesProvider {

  override def enums: Seq[TypesProviderEnum] = internal.enums.map { enum =>
    TypesProviderEnum(
      namespace = internal.namespace.getOrElse(""),
      name = enum.name,
      plural = enum.plural,
      values = enum.values.flatMap(_.name)
    )
  }

  override def unions: Seq[TypesProviderUnion] = internal.unions.map { u =>
    TypesProviderUnion(
      namespace = internal.namespace.getOrElse(""),
      name = u.name,
      plural = u.plural,
      types = u.types.flatMap { t =>
        t.datatype.toOption.map(_.name).map(core.TypesProviderUnionType)
      }
    )
  }

  override def models: Seq[TypesProviderModel] = internal.models.map { m =>
    TypesProviderModel(
      namespace = internal.namespace.getOrElse(""),
      name = m.name,
      plural = m.plural,
      fields = m.fields.
        filter(_.name.isDefined).
        flatMap { f =>
          f.datatype.toOption.map { dt =>
            TypesProviderField(
              name = f.name.get,
              `type` = dt.label,
            )
          }
      }
    )
  }

  override def interfaces: Seq[TypesProviderInterface] = internal.interfaces.map { i =>
    TypesProviderInterface(
      namespace = internal.namespace.getOrElse(""),
      name = i.name,
      plural = i.plural,
      fields = i.fields.
        filter(_.name.isDefined).
        flatMap { f =>
          f.datatype.toOption.map { dt =>
            TypesProviderField(
              name = f.name.get,
              `type` = dt.label,
            )
          }
        }
    )
  }
}

/**
  * Takes an internal service form and recursively builds up a type
  * provider for all enums and all models specified in the service or
  * in any of the imports. Takes care to avoid importing the same
  * service multiple times (based on uniqueness of the import URIs)
  */
private[api_json] case class RecursiveTypesProvider(
  internal: InternalServiceForm
) extends TypesProvider {

  override def enums: Seq[TypesProviderEnum] = providers.flatMap(_.enums)

  override def unions: Seq[TypesProviderUnion] = providers.flatMap(_.unions)

  override def models: Seq[TypesProviderModel] = providers.flatMap(_.models)

  override def interfaces: Seq[TypesProviderInterface] = providers.flatMap(_.interfaces)

  private lazy val providers = Seq(InternalServiceFormTypesProvider(internal)) ++ resolve(internal.imports.flatMap(_.uri))

  private def resolve(
    importUris: Seq[String],
    imported: List[String] = List.empty
  ): Seq[TypesProvider] = {
    importUris match {
      case Nil => Nil
      case uri :: rest => {
        if (imported.contains(uri.toLowerCase.trim)) {
          // already imported
          resolve(rest, imported)
        } else {
          val importer = Importer(internal.fetcher, uri)
          importer.validate match {
            case Valid(_) => {
              Seq(TypesProvider.FromService(importer.service)) ++ resolve(rest, imported ++ List(uri))
            }
            case Invalid(_) => {
              // There are errors w/ this import - skip it
              resolve(rest, imported ++ List(uri))
            }
          }
        }
      }
    }
  }

}

private[api_json] case class TypeResolver(
  defaultNamespace: Option[String],
  provider: TypesProvider
) {

  private val resolver = provider.resolver

  private lazy val validator = TypeValidator(
    defaultNamespace = defaultNamespace,
    provider.enums
  )

  /**
    * Resolves the type name into instances of a first class Type.
    */
  def parse(internal: InternalDatatype): Option[Kind] = {
    resolver.parse(internal.label)
  }

  def validate(
    kind: Kind,
    value: String,
    errorPrefix: Option[String] = None
  ): Option[String] = {
    validator.validate(kind, value, errorPrefix)
  }

}
