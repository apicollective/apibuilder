package db

import com.gilt.apidoc.v0.models.Visibility
import java.util.UUID
import anorm.NamedParameter

sealed trait Authorization {

  /**
    * Generates a sql filter to restrict the returned set of
    * organizations to those that are able to be seen by this
    * authorization.
    * 
    * @param organizationsTableName e.g "organizations"
    */
  def organizationFilter(
    organizationsTableName: String = "organizations"
  ): Option[String]

  /**
    * Generates a sql filter to restrict the returned set of
    * applications to those that are able to be seen by this
    * authorization.
    */
  def applicationFilter(
    applicationsTableName: String = "applications",
    organizationsTableName: String = "organizations"
  ): Option[String]

  def bindVariables(): Seq[NamedParameter] = Seq.empty

}

object Authorization {

  private val UserQuery =
    s"select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid"

  private val PublicApplicationsQuery = s"%s.visibility = '${Visibility.Public.toString}'"

  case object PublicOnly extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = {
      Some(s"$organizationsTableName.visibility = '${Visibility.Public}'")
    }

    def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      Some(PublicApplicationsQuery.format(applicationsTableName))
    }

  }

  case object All extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = None

    override def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = None

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = {
      Some(s"($organizationsTableName.visibility = '${Visibility.Public}' or $organizationsTableName.guid in ($UserQuery))")
    }

    override def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      Some(
        "(" + PublicApplicationsQuery.format(applicationsTableName) + " or " + organizationFilter(organizationsTableName).get + ")"
      )
    }

    override def bindVariables(): Seq[NamedParameter] = {
      Seq[NamedParameter](
        'authorization_user_guid -> userGuid.toString
      )
    }

  }

  def apply(user: Option[com.gilt.apidoc.v0.models.User]): Authorization = {
    user match {
      case None => PublicOnly
      case Some(u) => User(u.guid)
    }
  }

}
