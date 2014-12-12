package db

import com.gilt.apidoc.models.Visibility
import java.util.UUID
import anorm.NamedParameter

sealed trait Authorization {

  def organizationFilter(organizationGuidColumn: String): Option[String]

  def bindVariables(): Seq[NamedParameter] = Seq.empty

}

object Authorization {

  private val MetadataQuery =
    s"select organization_guid from organization_metadata where organization_metadata.deleted_at is null and organization_metadata.visibility = '${Visibility.Public}'"

  private val UserQuery =
    s"select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid"

  case object PublicOnly extends Authorization {

    override def organizationFilter(organizationGuidColumn: String) = {
      Some(s"$organizationGuidColumn in ($MetadataQuery)")
    }

  }

  case object All extends Authorization {

    override def organizationFilter(organizationGuidColumn: String) = None

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(organizationGuidColumn: String) = {
      Some(s"$organizationGuidColumn in ($MetadataQuery union all $UserQuery)")
    }

    override def bindVariables(): Seq[NamedParameter] = {
      Seq[NamedParameter](
        'authorization_user_guid -> userGuid.toString
      )
    }

  }

  def apply(user: Option[com.gilt.apidoc.models.User]): Authorization = {
    user match {
      case None => PublicOnly
      case Some(u) => User(u.guid)
    }
  }

}
