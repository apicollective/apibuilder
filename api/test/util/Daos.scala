package util

import db.generated.SessionsDao
import play.api.Application

trait Daos {
  def app: Application

  def applicationsDao = app.injector.instanceOf[db.ApplicationsDao]
  def attributesDao = app.injector.instanceOf[db.AttributesDao]
  def changesDao = app.injector.instanceOf[db.ChangesDao]
  def emailVerificationsDao = app.injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao = app.injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao = app.injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao = app.injector.instanceOf[db.MembershipsDao]
  def usersDao = app.injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao = app.injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao = app.injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao = app.injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao = app.injector.instanceOf[db.OrganizationsDao]
  def originalsDao = app.injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao = app.injector.instanceOf[db.PasswordResetRequestsDao]
  def subscriptionsDao = app.injector.instanceOf[db.SubscriptionsDao]
  def tasksDao = app.injector.instanceOf[db.TasksDao]
  def tokensDao = app.injector.instanceOf[db.TokensDao]
  def userPasswordsDao = app.injector.instanceOf[db.UserPasswordsDao]
  def versionsDao = app.injector.instanceOf[db.VersionsDao]

  def servicesDao = app.injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao = app.injector.instanceOf[db.generators.GeneratorsDao]

  def emails = app.injector.instanceOf[actors.Emails]
  def search = app.injector.instanceOf[actors.Search]

  def sessionsDao = app.injector.instanceOf[SessionsDao]
  def sessionHelper = app.injector.instanceOf[SessionHelper]
}
