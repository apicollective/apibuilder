package db

import com.gilt.apidoc.api.v0.models.Visibility
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

  /**
    * Generates a sql filter to restrict the returned set of
    * tokens to those that this user is authorized to access.
    * 
    * @param tokensTableName e.g "tokens"
    */
  def tokenFilter(
    tokensTableName: String = "tokens"
  ): Option[String]

  /**
    * Generates a sql filter to restrict the returned set of
    * subscriptions to those that this user is authorized to access.
    * 
    * @param subscriptionsTableName e.g "subscriptions"
    */
  def subscriptionFilter(
    subscriptionsTableName: String = "subscriptions",
    organizationsTableName: String = "organizations"
  ): Option[String]

  def bindVariables(): Seq[NamedParameter] = Seq.empty

}

object Authorization {

  private val OrgsByUserQuery =
    s"select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid"

  private val PublicApplicationsQuery = s"%s.visibility = '${Visibility.Public.toString}'"

  case object PublicOnly extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = {
      Some(s"$organizationsTableName.visibility = '${Visibility.Public}'")
    }

    override def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      Some(PublicApplicationsQuery.format(applicationsTableName))
    }

    override def tokenFilter(
      tokensTableName: String = "tokens"
    ): Option[String] = Some("false")

    override def subscriptionFilter(
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Option[String] = Some("false")

  }

  case object All extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = None

    override def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = None

    override def tokenFilter(
      tokensTableName: String = "tokens"
    ): Option[String] = None

    override def subscriptionFilter(
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Option[String] = None

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(
      organizationsTableName: String = "organizations"
    ) = {
      Some(s"($organizationsTableName.visibility = '${Visibility.Public}' or $organizationsTableName.guid in ($OrgsByUserQuery))")
    }

    override def applicationFilter(
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      Some(
        organizationFilter(organizationsTableName).get +
        " and (" + PublicApplicationsQuery.format(applicationsTableName) +
        s" or $organizationsTableName.guid in ($OrgsByUserQuery)) "
      )
    }

    override def tokenFilter(
      tokensTableName: String = "tokens"
    ): Option[String] = {
      Some(s"${tokensTableName}.user_guid = {authorization_user_guid}::uuid")
    }

    override def subscriptionFilter(
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Option[String] = {
      val orgsFilter = s"$organizationsTableName.guid in ($OrgsByUserQuery)"
      Some(s"(${subscriptionsTableName}.user_guid = {authorization_user_guid}::uuid or $orgsFilter)")
    }

    override def bindVariables(): Seq[NamedParameter] = {
      Seq[NamedParameter](
        'authorization_user_guid -> userGuid.toString
      )
    }

  }

  def apply(userGuid: Option[UUID]): Authorization = {
    userGuid match {
      case None => PublicOnly
      case Some(guid) => User(guid)
    }
  }

}
