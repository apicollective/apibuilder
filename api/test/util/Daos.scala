package util

import actors.Emails
import db.{ApplicationsDao, AttributesDao, ChangesDao, EmailVerificationsDao, ItemsDao, MembershipRequestsDao, MembershipsDao, OrganizationAttributeValuesDao, OrganizationDomainsDao, OrganizationLogsDao, OrganizationsDao, OriginalsDao, PasswordResetRequestsDao, SubscriptionsDao, TasksDao, TokensDao, UserPasswordsDao, UsersDao, VersionsDao}
import db.generated.SessionsDao
import db.generators.{GeneratorsDao, ServicesDao}
import lib.DatabaseServiceFetcher
import play.api.Application
import play.api.inject.Injector

trait Daos {
  def app: Application
  def injector: Injector = app.injector

  def applicationsDao: ApplicationsDao = injector.instanceOf[db.ApplicationsDao]
  def attributesDao: AttributesDao = injector.instanceOf[db.AttributesDao]
  def changesDao: ChangesDao = injector.instanceOf[db.ChangesDao]
  def databaseServiceFetcher: DatabaseServiceFetcher = injector.instanceOf[DatabaseServiceFetcher]
  def emailVerificationsDao: EmailVerificationsDao = injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao: ItemsDao = injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao: MembershipRequestsDao = injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao: MembershipsDao = injector.instanceOf[db.MembershipsDao]
  def usersDao: UsersDao = injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao: OrganizationAttributeValuesDao = injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao: OrganizationDomainsDao = injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao: OrganizationLogsDao = injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao: OrganizationsDao = injector.instanceOf[db.OrganizationsDao]
  def originalsDao: OriginalsDao = injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao: PasswordResetRequestsDao = injector.instanceOf[db.PasswordResetRequestsDao]
  def sessionsDao: SessionsDao =  injector.instanceOf[SessionsDao]

  def subscriptionsDao: SubscriptionsDao = injector.instanceOf[db.SubscriptionsDao]
  def tasksDao: TasksDao = injector.instanceOf[db.TasksDao]
  def tokensDao: TokensDao = injector.instanceOf[db.TokensDao]
  def userPasswordsDao: UserPasswordsDao = injector.instanceOf[db.UserPasswordsDao]
  def versionsDao: VersionsDao = injector.instanceOf[db.VersionsDao]

  def servicesDao: ServicesDao = injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao: GeneratorsDao = injector.instanceOf[db.generators.GeneratorsDao]

  def emails: Emails = injector.instanceOf[actors.Emails]
  def search: Search = injector.instanceOf[actors.Search]

  def sessionHelper: SessionHelper = injector.instanceOf[SessionHelper]
}
