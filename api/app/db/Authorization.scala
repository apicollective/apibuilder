package db

import com.gilt.apidoc.models.Visibility
import java.util.UUID
import anorm.NamedParameter

sealed trait Authorization {

  /**
    * Generates a sql filter to restrict the returned set of
    * organizations to those that are able to be seen by this
    * authorization.
    * 
    * @param organizationGuidColumn e.g "organizations.guid"
    * @param organizationMetadataTableName e.g "organization_metadata" - if speciied,
    *        we can avoid the subquery on the metadata table
    */
  def organizationFilter(
    organizationGuidColumn: String,
    organizationMetadataTableName: Option[String] = None
  ): Option[String]

  /**
    * Generates a sql filter to restrict the returned set of
    * applications to those that are able to be seen by this
    * authorization.
    */
  def applicationFilter(
    applicationsTableName: String = "applications"
  ): Option[String]

  def bindVariables(): Seq[NamedParameter] = Seq.empty

}

object Authorization {

  private val MetadataQuery =
    s"select organization_guid from organization_metadata where organization_metadata.deleted_at is null and organization_metadata.visibility = '${Visibility.Public}'"

  private val UserQuery =
    s"select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid"

  private val PublicApplicationsQuery = s"%s.visibility = '${Visibility.Public.toString}'"

  case object PublicOnly extends Authorization {

    override def organizationFilter(
      organizationGuidColumn: String,
      organizationMetadataTableName: Option[String] = None
    ) = {
      organizationMetadataTableName match {
        case None => Some(s"$organizationGuidColumn in ($MetadataQuery)")
        case Some(table) => Some(s"$table.visibility = '${Visibility.Public}'")
      }
    }

    def applicationFilter(
      applicationsTableName: String = "applications"
    ) = {
      Some(PublicApplicationsQuery.format(applicationsTableName))
    }

  }

  case object All extends Authorization {

    override def organizationFilter(
      organizationGuidColumn: String,
      organizationMetadataTableName: Option[String] = None
    ) = None

    def applicationFilter(
      applicationsTableName: String = "applications"
    ) = None

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(
      organizationGuidColumn: String,
      organizationMetadataTableName: Option[String] = None
    ) = {
      organizationMetadataTableName match {
        case None => Some(s"$organizationGuidColumn in ($MetadataQuery union all $UserQuery)")
        case Some(table) => Some(s"($table.visibility = '${Visibility.Public}' or $organizationGuidColumn in ($UserQuery))")
      }
    }

    def applicationFilter(
      applicationsTableName: String = "applications"
    ) = {
      Some(
        "(" + PublicApplicationsQuery.format(applicationsTableName) + " or " + organizationFilter(s"$applicationsTableName.organization_guid").get + ")"
      )
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
