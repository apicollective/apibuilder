package models

import db.{ Organization, ServiceDao, User, VersionDao }
import io.Source
import java.util.UUID

/**
 * Example service for tests
 */
object IrisHubService {

  new play.core.StaticApplication(new java.io.File("."))

  private lazy val user = User.upsert("otto@gilttest.com")
  private lazy val orgName = "Test Org %s".format(UUID.randomUUID)
  private lazy val org = Organization.findByKey(orgName).getOrElse {
    Organization.create(user, orgName)
  }

  private val serviceDao = ServiceDao.upsert(user, org, "Iris Hub")
  private val contents = Source.fromFile("/web/svc-iris-hub/api.json").mkString
  private val versionDao = VersionDao.upsert(serviceDao, "1.0.0", contents)
  val service = Service(org, serviceDao, versionDao)

  val vendor = service.resource("vendor").get
  val get = vendor.operation("GET").get
  val delete = vendor.operation("DELETE", Some("/:guid")).get

}

