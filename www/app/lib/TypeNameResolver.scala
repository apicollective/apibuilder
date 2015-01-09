package lib

case class TypeNameResolution(
  namespace: String,
  kind: Kind,
  name: String
)

case class TypeNameResolver(value: String) {

  private val EnumRx = "^(.+)\\.enums\\.(.+)$".r
  private val ModelRx = "^(.+)\\.models\\.(.+)$".r

  def resolve(): Option[TypeNameResolution] = {
    value match {
      case EnumRx(namespace, name)  => {
        Some(TypeNameResolution(namespace, Kind.Enum, name))
      }
      case ModelRx(namespace, name)  => {
        Some(TypeNameResolution(namespace, Kind.Model, name))
      }
      case _ => None
    }
  }

}
