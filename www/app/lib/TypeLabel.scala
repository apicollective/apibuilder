package lib

import com.bryzek.apidoc.api.v0.models.{Application, Organization}
import com.bryzek.apidoc.spec.v0.models.{Import, Service}

case class TypeLabel(
  org: Organization,
  app: Application,
  version: String,
  service: Service,
  typeName: String
) {

  val link = DatatypeResolver(
    enumNames = service.enums.map(_.name),
    modelNames = service.models.map(_.name),
    unionNames = service.unions.map(_.name)
  ).parse(typeName) match {
    case None => {
      TextDatatype(typeName) match {
        case TextDatatype.List(t) => resolveImportedType(t) match {
          case None => typeName
          case Some(impType) => s"[$impType]"
        }
        case TextDatatype.Map(t) => resolveImportedType(t) match {
          case None => typeName
          case Some(impType) => s"map[$impType]"
        }
        case TextDatatype.Singleton(t) => resolveImportedType(t).getOrElse(typeName)
      }
    }
    case Some(Datatype.List(t)) => "[" + typeLink(t) + "]"
    case Some(Datatype.Map(t)) => "map[" + typeLink(t) + "]"
    case Some(Datatype.Singleton(t)) => typeLink(t)
  }

  private[this] def typeLink(t: Type): String = {
    Href(org.key, app.key, version, t).html
  }

  private[this] def resolveImportedType(typeName: String): Option[String] = {
    service.imports.flatMap { imp =>
      imp.enums.find { name => s"${imp.namespace}.models.$name" == typeName } match {
        case Some(shortName) => Some(importLink(imp, Kind.Enum, shortName, typeName))
        case None => {
          imp.models.find { name => s"${imp.namespace}.models.$name" == typeName } match {
            case Some(shortName) => Some(importLink(imp, Kind.Model, shortName, typeName))
            case None => {
              imp.unions.find { name => s"${imp.namespace}.models.$name" == typeName } match {
                case Some(shortName) => Some(importLink(imp, Kind.Union, shortName, typeName))
                case None => None
              }
            }
          }
        }
      }
    }.headOption
  }

  private[this] def importLink(imp: Import, kind: Kind, shortName: String, fullName: String): String = {
    s"<a href='/${imp.organization.key}/${imp.application.key}/${imp.version}#$kind-$shortName'>$fullName:${imp.version}</a>"

  }

}

case class Href(label: String, url: String) {
  def html: String = s"""<a href="$url">$label</a>"""
}

object Href {

  def prefix(orgKey: String, appKey: String, version: String): String = {
    s"/${orgKey}/${appKey}/${version}"
  }

  def apply(orgKey: String, appKey: String, version: String, t: Type): Href = {
    t match {
      case Type(Kind.Primitive, name) => {
        Href(name, s"/doc/types#${UrlKey.generate(name)}")
      }
      case Type(Kind.Enum, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#enum-${UrlKey.generate(name)}")
      }
      case Type(Kind.Model, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#model-${UrlKey.generate(name)}")
      }
      case Type(Kind.Union, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#union-${UrlKey.generate(name)}")
      }
    }
  }

}

