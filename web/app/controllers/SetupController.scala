package controllers

import lib.Path
import models.Service
import db.{ Organization, OrganizationQuery, Membership, ServiceDao, User, VersionDao }
import io.Source

import play.api._
import play.api.mvc._

object SetupController extends Controller {

  private def updateAttributes(user: User, org: Organization, service: ServiceDao, version: VersionDao) {
    val s = Service(org, service, version)
    println("Updating name to "+ s.jsonName)
    ServiceDao.update(user, service.copy(description = s.description, name = s.jsonName))
  }

  def index() = Action { request =>
    val user = User.upsert("mbryzek@yahoo.com", Some("Michael Bryzek"), None)

    val org = Organization.findByKey("Gilt").getOrElse {
      Organization.create(user, "Gilt")
    }

    Membership.upsert(user, org, user, "admin")

    val irisHubService = ServiceDao.upsert(user, org, "Iris Hub")
    val irisHubVersionDao = VersionDao.upsert(irisHubService, "1.0.0", Source.fromFile("/web/svc-iris-hub/api.json").mkString)
    updateAttributes(user, org, irisHubService, irisHubVersionDao)

    val apiService = ServiceDao.upsert(user, org, "API Server")
    VersionDao.upsert(apiService, "0.5", Source.fromFile("./api.json").mkString)
    val apiVersionDao = VersionDao.upsert(apiService, "0.6", Source.fromFile("./api.json").mkString)
    updateAttributes(user, org, apiService, apiVersionDao)

    val builderService = ServiceDao.upsert(user, org, "BUILDER Server")
    val builderVersionDao = VersionDao.upsert(builderService, "0.6", Source.fromFile("/web/cavellc/builder/api.json").mkString)
    updateAttributes(user, org, builderService, builderVersionDao)

    Redirect("/").withSession { "user_guid" -> user.guid.toString }
  }

}
