package lib

case class TypeNameResolution(
  orgNamespace: String,
  applicationKey: String,
  kind: TypeKind,
  name: String
)

case class TypeNameResolver(name: String) {

  private val EnumRx = "^(.+)\\.(.+)\\.enums\\.(.+)$".r
  private val ModelRx = "^(.+)\\.(.+)\\.models\\.(.+)$".r

  def resolve(): Option[TypeNameResolution] = {
    name match {
      case EnumRx(orgNamespace, applicationKey, n)  => {
        Some(TypeNameResolution(orgNamespace, applicationKey, TypeKind.Enum, n))
      }
      case ModelRx(orgNamespace, applicationKey, n)  => {
        Some(TypeNameResolution(orgNamespace, applicationKey, TypeKind.Model, n))
      }
      case _ => None
    }
  }

}
