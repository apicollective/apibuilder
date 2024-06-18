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

case class Table(schema: String, name: String, pkey: PrimaryKey) {
  def qualified: String = s"$schema.$name"
}
object Table {
  def public(name: String, pkey: PrimaryKey): Table = Table("public", name, pkey)
  def guid(schema: String, name: String): Table = Table(schema, name, PrimaryKey.PkeyUUID)
  def string(schema: String, name: String): Table = Table(schema, name, PrimaryKey.PkeyString)
  def long(schema: String, name: String): Table = Table(schema, name, PrimaryKey.PkeyLong)
}

object Tables {
  val organizations: Table = Table.public("organizations", PrimaryKey.PkeyUUID)
  val applications: Table = Table.public("applications", PrimaryKey.PkeyUUID)
  val versions: Table = Table.public("versions", PrimaryKey.PkeyUUID)
}
