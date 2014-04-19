package core

case class Role(key: String, name: String)

object Role {

  val Admin = Role("admin", "Admin")
  val Member = Role("member", "Member")

  def fromString(role: String): Option[Role] = {
    if (Admin.key == role) {
      Some(Admin)
    } else if (Member.key == role) {
      Some(Member)
    } else {
      None
    }
  }

}
