package util

import db.generated.SessionsDao

trait Daos {

  def applicationsDao = play.api.Play.current.injector.instanceOf[db.ApplicationsDao]
  def attributesDao = play.api.Play.current.injector.instanceOf[db.AttributesDao]
  def changesDao = play.api.Play.current.injector.instanceOf[db.ChangesDao]
  def emailVerificationsDao = play.api.Play.current.injector.instanceOf[db.EmailVerificationsDao]
  def itemsDao = play.api.Play.current.injector.instanceOf[db.ItemsDao]
  def membershipRequestsDao = play.api.Play.current.injector.instanceOf[db.MembershipRequestsDao]
  def membershipsDao = play.api.Play.current.injector.instanceOf[db.MembershipsDao]
  def usersDao = play.api.Play.current.injector.instanceOf[db.UsersDao]

  def organizationAttributeValuesDao = play.api.Play.current.injector.instanceOf[db.OrganizationAttributeValuesDao]
  def organizationDomainsDao = play.api.Play.current.injector.instanceOf[db.OrganizationDomainsDao]
  def organizationLogsDao = play.api.Play.current.injector.instanceOf[db.OrganizationLogsDao]
  def organizationsDao = play.api.Play.current.injector.instanceOf[db.OrganizationsDao]
  def originalsDao = play.api.Play.current.injector.instanceOf[db.OriginalsDao]
  def passwordResetRequestsDao = play.api.Play.current.injector.instanceOf[db.PasswordResetRequestsDao]
  def sessionsDao =  play.api.Play.current.injector.instanceOf[SessionsDao]

  def subscriptionsDao = play.api.Play.current.injector.instanceOf[db.SubscriptionsDao]
  def tasksDao = play.api.Play.current.injector.instanceOf[db.TasksDao]
  def tokensDao = play.api.Play.current.injector.instanceOf[db.TokensDao]
  def userPasswordsDao = play.api.Play.current.injector.instanceOf[db.UserPasswordsDao]
  def versionsDao = play.api.Play.current.injector.instanceOf[db.VersionsDao]

  def servicesDao = play.api.Play.current.injector.instanceOf[db.generators.ServicesDao]
  def generatorsDao = play.api.Play.current.injector.instanceOf[db.generators.GeneratorsDao]

  def emails = play.api.Play.current.injector.instanceOf[actors.Emails]
  def search = play.api.Play.current.injector.instanceOf[actors.Search]

  def sessionHelper = play.api.Play.current.injector.instanceOf[SessionHelper]
}
