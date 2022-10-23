package lib

import io.apibuilder.api.v0.models.{Application, Organization}
import io.apibuilder.spec.v0.models.{Import, Service}
import play.api.Logger

case class TypeLabel(
  org: Organization,
  app: Application,
  version: String,
  service: Service,
  typeName: String
) {

  private[this] val logger: Logger = Logger(this.getClass)

  private[this] val localResolver = DatatypeResolver(
    enumNames = service.enums.map(_.name),
    interfaceNames = service.interfaces.map(_.name),
    modelNames = service.models.map(_.name),
    unionNames = service.unions.map(_.name)
  )

  // The last type will be the concrete type. All other types are
  // wrappers (e.g. Map, List)
  private[this] val types = TextDatatype.parse(typeName)

  private[this] val typeLink = types.lastOption match {
    case None => {
      // Unknown type - just display the type name w/out a link
      typeName
    }

    case Some(t) => {
      t match {
        case TextDatatype.List | TextDatatype.Map => {
          logger.error(s"Unexpected list or map type when resolving typeName[$typeName]")
          typeName
        }
        case TextDatatype.Singleton(name) => {
          localResolver.parse(name).map { kind => Href(org.key, app.key, version, kind).html }.getOrElse {
            importedTypeLink(name).getOrElse(typeName)
          }
        }
      }
    }
  }

  val link: String = buildLabel(typeLink, types.reverse.drop(1))

  @scala.annotation.tailrec
  private[this] def buildLabel(label: String, types: Seq[TextDatatype]): String = {
    types match {
      case Nil => {
        label
      }
      case next :: rest => {
        next match {
          case TextDatatype.List => buildLabel(s"[$label]", rest)
          case TextDatatype.Map => buildLabel(s"map[$label]", rest)
          case TextDatatype.Singleton(name) => sys.error(s"Unexpected singleton when building link for label[$label] types[$types]")
        }
      }
    }
  }

  /**
    * Looks for the specified type in the imports for this service. If
    * found, returns a link to that type.
    */
  private[this] def importedTypeLink(typeName: String): Option[String] = {
    service.imports.flatMap { imp =>
      imp.enums.find { name => s"${imp.namespace}.enums.$name" == typeName } match {
        case Some(shortName) => {
          Some(importLink(imp, "enum", shortName, typeName))
        }

        case None => {
          imp.models.find { name => s"${imp.namespace}.models.$name" == typeName } match {
            case Some(shortName) => {
              Some(importLink(imp, "model", shortName, typeName))
            }

            case None => {
              imp.unions.find { name => s"${imp.namespace}.unions.$name" == typeName } match {
                case Some(shortName) => {
                  Some(importLink(imp, "union", shortName, typeName))
                }

                case None => {
                  None
                }
              }
            }
          }
        }
      }
    }.headOption
  }

  private[this] def importLink(imp: Import, kind: String, shortName: String, fullName: String): String = {
    Href(
      s"$fullName:${imp.version}",
      Href.prefix(imp.organization.key, imp.application.key, imp.version) + s"#$kind-${UrlKey.generate(shortName)}"
    ).html
  }

}

case class Href(label: String, url: String) {
  def html: String = s"""<a href="$url">$label</a>"""
}

object Href {

  def prefix(orgKey: String, appKey: String, version: String): String = {
    s"/${orgKey}/${appKey}/${version}"
  }

  def apply(orgKey: String, appKey: String, version: String, kind: Kind): Href = {
    kind match {
      case Kind.Primitive(name) => {
        Href(name, s"/doc/types#${UrlKey.generate(name)}")
      }
      case Kind.Enum(name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#enum-${UrlKey.generate(name)}")
      }
      case Kind.Model(name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#model-${UrlKey.generate(name)}")
      }
      case Kind.Interface(name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#interface-${UrlKey.generate(name)}")
      }
      case Kind.Union(name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#union-${UrlKey.generate(name)}")
      }
      case Kind.List(_) | Kind.Map(_) => {
        sys.error(s"Did not expect list or map as final type for ${prefix(orgKey, appKey, version)} kind[$kind]")
      }
    }
  }

}

