package util

sealed trait PrimaryKey {
  def name: String
}
object PrimaryKey {
  case object PkeyString extends PrimaryKey {
    override val name = "id"
  }
  case object PkeyLong extends PrimaryKey {
    override val name = "id"
  }
  case object PkeyUUID extends PrimaryKey {
    override val name = "guid"
  }
}

case class TableMetadata(name: String, pkey: PrimaryKey)
object TableMetadata {
  def guid(name: String): TableMetadata = TableMetadata(name, PrimaryKey.PkeyUUID)
  def string(name: String): TableMetadata = TableMetadata(name, PrimaryKey.PkeyString)
  def long(name: String): TableMetadata = TableMetadata(name, PrimaryKey.PkeyLong)
}

object Tables {
  val organizations: TableMetadata = TableMetadata("organizations", PrimaryKey.PkeyUUID)
  val applications: TableMetadata = TableMetadata("applications", PrimaryKey.PkeyUUID)
  val versions: TableMetadata = TableMetadata("versions", PrimaryKey.PkeyUUID)
}
