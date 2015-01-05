package lib

case class TypeNameResolution(
  orgNamespace: String,
  serviceKey: String,
  kind: TypeKind,
  name: String
)

case class TypeNameResolver(name: String) {

  private val EnumRx = "^(.+)\\.(.+)\\.enums\\.(.+)$".r
  private val ModelRx = "^(.+)\\.(.+)\\.models\\.(.+)$".r

  def resolve(): Option[TypeNameResolution] = {
    name match {
      case EnumRx(orgNamespace, serviceKey, n)  => {
        Some(TypeNameResolution(orgNamespace, serviceKey, TypeKind.Enum, n))
      }
      case ModelRx(orgNamespace, serviceKey, n)  => {
        Some(TypeNameResolution(orgNamespace, serviceKey, TypeKind.Model, n))
      }
      case _ => None
    }
  }

}
