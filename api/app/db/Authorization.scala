package db

import io.apibuilder.api.v0.models.Visibility
import io.flow.postgresql.Query
import java.util.UUID

sealed trait Authorization {

  /**
    * Generates a sql filter to restrict the returned set of
    * organizations to those that are able to be seen by this
    * authorization.
    * 
    * @param organizationsTableName e.g "organizations"
    */
  def organizationFilter(
    query: Query,
    organizationsTableName: String = "organizations"
  ): Query

  /**
    * Generates a sql filter to restrict the returned set of
    * applications to those that are able to be seen by this
    * authorization.
    */
  def applicationFilter(
    query: Query,
    applicationsTableName: String = "applications",
    organizationsTableName: String = "organizations"
  ): Query

  /**
    * Generates a sql filter to restrict the returned set of
    * tokens to those that this user is authorized to access.
    * 
    * @param tokensTableName e.g "tokens"
    */
  def tokenFilter(
    query: Query,
    tokensTableName: String = "tokens"
  ): Query

  /**
    * Generates a sql filter to restrict the returned set of
    * subscriptions to those that this user is authorized to access.
    * 
    * @param subscriptionsTableName e.g "subscriptions"
    */
  def subscriptionFilter(
    query: Query,
    subscriptionsTableName: String = "subscriptions",
    organizationsTableName: String = "organizations"
  ): Query

  /**
    * Generates a sql filter to restrict the returned set of generator
    * services to those that this user is authorized to access. At the
    * moment this is a placeholder as we currently do NOT restrict access
    * to code generators.
    * 
    * @param generatorServicessTableName e.g "services"
    */
  def generatorServicesFilter(
    query: Query,
    generatorServicessTableName: String = "services"
  ): Query = query

}

object Authorization {

  private val OrgsByUserQuery =
    s"select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid"

  private val PublicApplicationsQuery = s"%s.visibility = '${Visibility.Public.toString}'"

  case object PublicOnly extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationsTableName: String = "organizations"
    ) = {
      query.equals(s"$organizationsTableName.visibility", Visibility.Public.toString)
    }

    override def applicationFilter(
      query: Query,
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      query.equals(s"$applicationsTableName.visibility", Visibility.Public.toString)
    }

    override def tokenFilter(
      query: Query,
      tokensTableName: String = "tokens"
    ): Query = query.and("false")

    override def subscriptionFilter(
      query: Query,
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Query = query.and("false")

  }

  case object All extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationsTableName: String = "organizations"
    ) = query

    override def applicationFilter(
      query: Query,
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = query

    override def tokenFilter(
      query: Query,
      tokensTableName: String = "tokens"
    ): Query = query

    override def subscriptionFilter(
      query: Query,
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Query = query

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationsTableName: String = "organizations"
    ) = {
      query.or(
        List(
          s"($organizationsTableName.visibility = '${Visibility.Public}'",
          s"$organizationsTableName.guid in ($OrgsByUserQuery))"
        )
      ).bind("authorization_user_guid", userGuid)
    }

    override def applicationFilter(
      query: Query,
      applicationsTableName: String = "applications",
      organizationsTableName: String = "organizations"
    ) = {
      organizationFilter(query, organizationsTableName).or(
        List(
          PublicApplicationsQuery.format(applicationsTableName),
          s"$organizationsTableName.guid in ($OrgsByUserQuery)"
        )
      )
    }

    override def tokenFilter(
      query: Query,
      tokensTableName: String = "tokens"
    ): Query = {
      query.equals(s"${tokensTableName}.user_guid", userGuid)
    }

    override def subscriptionFilter(
      query: Query,
      subscriptionsTableName: String = "subscriptions",
      organizationsTableName: String = "organizations"
    ): Query = {
      val orgsFilter = s"$organizationsTableName.guid in ($OrgsByUserQuery)"
      query.and(s"(${subscriptionsTableName}.user_guid = {authorization_user_guid}::uuid or $orgsFilter)").
        bind("authorization_user_guid", userGuid)
    }

  }

  def apply(userGuid: Option[UUID]): Authorization = {
    userGuid match {
      case None => PublicOnly
      case Some(guid) => User(guid)
    }
  }

}
