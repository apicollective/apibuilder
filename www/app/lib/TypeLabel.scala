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

  private val urlPrefix = s"/${org.key}/${app.key}/${version}"

  val link = DatatypeResolver(
    enumNames = service.enums.map(_.name),
    modelNames = service.models.map(_.name)
  ).parse(typeName) match {
    case None => {
      href(typeName, s"/types/resolve/$typeName")
    }
    case Some(Datatype.List(t)) => "[" + types(t) + "]"
    case Some(Datatype.Map(t)) => "map[" + types(t) + "]"
    case Some(Datatype.Option(t)) => "option[" + types(t) + "]"
    case Some(Datatype.Singleton(t)) => types(t)
  }

  private def types(types: Seq[Type]): String = {
    types.map { t =>
      t match {
        case Type(TypeKind.Primitive, name) => {
          href(name, s"/doc/types#$name")
        }
        case Type(TypeKind.Enum, name) => {
          href(name, s"$urlPrefix#enum-$name")
        }
        case Type(TypeKind.Model, name) => {
          href(name, s"$urlPrefix#model-$name")
        }
      }
    }.mkString(" | ")
  }

  private def href(label: String, url: String) = s"""<a href="$url">$label</a>"""

}

