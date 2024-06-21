package util

import io.apibuilder.task.v0.models.MembershipRole

case class Role(key: MembershipRole, name: String)

object Role {

  val Admin: Role = Role(MembershipRole.Admin, "Admin")
  val Member: Role = Role(MembershipRole.Member, "Member")

  val All: Seq[Role] = Seq(Member, Admin)

  def fromString(key: String): Option[Role] = {
    MembershipRole.fromString(key).flatMap {
      case MembershipRole.Member => Some(Member)
      case MembershipRole.Admin => Some(Admin)
      case MembershipRole.UNDEFINED(_) => None
    }
  }

}
