package lib

import com.gilt.apidoc.models.{Application, Organization}
import com.gilt.apidocspec.models.Service

case class TypeLabel(
  org: Organization,
  app: Application,
  version: String,
  service: Service,
  typeName: String
) {

  val link = DatatypeResolver(
    enumNames = service.enums.map(_.name),
    modelNames = service.models.map(_.name)
  ).parse(typeName) match {
    case None => {
      TypeNameResolver(typeName).resolve match {
        case None => typeName
        case Some(res) => {
          service.imports.find { _.namespace == res.namespace } match {
            case None => typeName
            case Some(imp) => {
              // handle list/map/option/singleton here
              s"<a href='/${imp.organizationKey}/${imp.applicationKey}/${imp.version}'>$typeName:$version</a>"
            }
          }
        }
      }
    }
    case Some(Datatype.List(t)) => "[" + types(t) + "]"
    case Some(Datatype.Map(t)) => "map[" + types(t) + "]"
    case Some(Datatype.Option(t)) => "option[" + types(t) + "]"
    case Some(Datatype.Singleton(t)) => types(t)
  }

  private def types(types: Seq[Type]): String = {
    types.map { t => Href(org.key, app.key, version, t).html }.mkString(" | ")
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
        Href(name, s"/doc/types#$name")
      }
      case Type(Kind.Enum, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#enum-$name")
      }
      case Type(Kind.Model, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#model-$name")
      }
    }
  }

}

