package lib

case class Role(key: String, name: String)

object Role {

  val Admin: Role = Role("admin", "Admin")
  val Member: Role = Role("member", "Member")

  val All: Seq[Role] = Seq(Member, Admin)

  def fromString(key: String): Option[Role] = {
    val lowerKey = key.toLowerCase
    All.find(_.key == lowerKey)
  }

}
