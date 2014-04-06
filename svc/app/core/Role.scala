package core

case class Role(key: String, name: String)

object Role {

  val Admin = Role("admin", "Admin")
  val Member = Role("member", "Member")

}
