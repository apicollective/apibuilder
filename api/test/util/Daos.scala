package util

import db.generated.SessionsDao
import lib.DatabaseServiceFetcher
import play.api.Application
import play.api.inject.Injector

trait Daos {
  def app: Application
  def injector: Injector = app.injector

  def applicationsDao = injector.instanceOf[db.ApplicationsDao]
  def attributesDao = injector.instanceOf[db.AttributesDao]
  def changesDao = injector.instanceOf[db.ChangesDao]
  def databaseServiceFetcher = injector.instanceOf[DatabaseServiceFetcher]
  def emailVerificationsDao = injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao = injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao = injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao = injector.instanceOf[db.MembershipsDao]
  def usersDao = injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao = injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao = injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao = injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao = injector.instanceOf[db.OrganizationsDao]
  def originalsDao = injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao = injector.instanceOf[db.PasswordResetRequestsDao]
  def sessionsDao =  injector.instanceOf[SessionsDao]

  def subscriptionsDao = injector.instanceOf[db.SubscriptionsDao]
  def tasksDao = injector.instanceOf[db.TasksDao]
  def tokensDao = injector.instanceOf[db.TokensDao]
  def userPasswordsDao = injector.instanceOf[db.UserPasswordsDao]
  def versionsDao = injector.instanceOf[db.VersionsDao]

  def servicesDao = injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao = injector.instanceOf[db.generators.GeneratorsDao]

  def emails = injector.instanceOf[actors.Emails]
  def search = injector.instanceOf[actors.Search]

  def sessionHelper = injector.instanceOf[SessionHelper]
}
