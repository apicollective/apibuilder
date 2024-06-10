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

case class Table(name: String, pkey: PrimaryKey)
object Table {
  def guid(name: String): Table = Table(name, PrimaryKey.PkeyUUID)
  def string(name: String): Table = Table(name, PrimaryKey.PkeyString)
  def long(name: String): Table = Table(name, PrimaryKey.PkeyLong)
}

object Tables {
  val organizations: Table = Table("organizations", PrimaryKey.PkeyUUID)
  val applications: Table = Table("applications", PrimaryKey.PkeyUUID)
  val versions: Table = Table("versions", PrimaryKey.PkeyUUID)
}
