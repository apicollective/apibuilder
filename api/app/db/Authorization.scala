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
    * @param organizationGuidColumnName e.g "organizations.guid"
    */
  def organizationFilter(
    query: Query,
    organizationGuidColumnName: String
  ): Query

  /**
    * Generates a sql filter to restrict the returned set of
    * applications to those that are able to be seen by this
    * authorization.
    */
  def applicationFilter(
    query: Query,
    applicationGuidColumnName: String
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
    organizationGuidColumnName: String
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

  private val OrgsByUserQuery: String =
    s"""
      |select organization_guid from memberships where memberships.deleted_at is null and memberships.user_guid = {authorization_user_guid}::uuid
      |""".stripMargin

  private def publicQuery(table: String): Query = {
    Query(
      s"select guid from $table"
    ).equals("visibility", Visibility.Public.toString)
  }

  private val PublicApplicationsQuery: Query = publicQuery("applications")
  private val PublicOrganizationsQuery: Query = publicQuery("organizations")

  case object PublicOnly extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationGuidColumnName: String
    ): Query = {
      query.in(organizationGuidColumnName, PublicOrganizationsQuery)
    }

    override def applicationFilter(
      query: Query,
      applicationGuidColumnName: String
    ): Query = {
      query.in(applicationGuidColumnName, PublicApplicationsQuery)
    }

    override def tokenFilter(
      query: Query,
      tokensTableName: String = "tokens"
    ): Query = query.and("false")

    override def subscriptionFilter(
      query: Query,
      subscriptionsTableName: String = "subscriptions",
      organizationGuidColumnName: String
    ): Query = query.and("false")

  }

  case object All extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationGuidColumnName: String
    ): Query = query

    override def applicationFilter(
      query: Query,
      applicationGuidColumnName: String
    ): Query = query

    override def tokenFilter(
      query: Query,
      tokensTableName: String = "tokens"
    ): Query = query

    override def subscriptionFilter(
      query: Query,
      subscriptionsTableName: String = "subscriptions",
      organizationGuidColumnName: String
    ): Query = query

  }

  case class User(userGuid: UUID) extends Authorization {

    override def organizationFilter(
      query: Query,
      organizationGuidColumnName: String
    ): Query = {
      query.in(organizationGuidColumnName,
        s"""
          |${PublicOrganizationsQuery.interpolate()}
          |UNION ALL
          |$OrgsByUserQuery
          |""".stripMargin
      ).withDebugging()
       .bind("authorization_user_guid", userGuid)
    }

    override def applicationFilter(
      query: Query,
      applicationGuidColumnName: String
    ): Query = {
      query.or(
        List(
          s"${applicationGuidColumnName} in ${PublicApplicationsQuery.interpolate()}",
          s"""
             |${applicationGuidColumnName} in (
             |select guid
             |  from applications
             | where organization_guid in ($OrgsByUserQuery)
             |    or organization_guid in (select guid from organizations where visibility = '${Visibility.Public}')
             |)
           """
        )
      ).bind("authorization_user_guid", userGuid)
        .withDebugging()
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
      organizationGuidColumnName: String
    ): Query = {
      query.and(s"(${subscriptionsTableName}.user_guid = {authorization_user_guid}::uuid or ${subscriptionsTableName}.organization_guid in (${OrgsByUserQuery})").
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
