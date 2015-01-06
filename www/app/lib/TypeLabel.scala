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
      Href(typeName, s"/types/resolve/$typeName?version=$version").html
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
      case Type(TypeKind.Primitive, name) => {
        Href(name, s"/doc/types#$name")
      }
      case Type(TypeKind.Enum, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#enum-$name")
      }
      case Type(TypeKind.Model, name) => {
        Href(name, prefix(orgKey, appKey, version) + s"#model-$name")
      }
    }
  }

}

