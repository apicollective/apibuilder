package controllers

import models.{ MainTemplate, Resource, Service }
import lib.{ OperationKey, Path }
import db.{ Organization, ServiceDao, VersionDao, ServiceQuery }

import play.api._
import play.api.mvc._

object ServiceController extends Controller {

  def redirect(orgKey: String) = Authenticated { request =>
    Organization.findByKey(orgKey) match {
      case None => Redirect("/")
      case Some(org: Organization) => Redirect(Path.url(org))
    }
  }

  def redirectToLatestVersion(orgKey: String, serviceKey: String) = Authenticated { request =>
    Redirect(s"/${orgKey}/docs/${serviceKey}/latest")
  }

  def latest(orgKey: String, serviceKey: String) = Authenticated { request =>
    getOptionalService(orgKey, serviceKey) match {
      case None => Ok(views.html.services.noversion(request.user, serviceKey))
      case Some(s: Service) => {
        Ok(views.html.services.show(request.user, s))
      }
    }
  }

  def show(orgKey: String, serviceKey: String, version: String) = Authenticated { request =>
    getOptionalService(orgKey, serviceKey, Some(version)) match {
      case Some(s: Service) => Ok(views.html.services.show(request.user, s))
      case None => Ok(views.html.services.noversion(request.user, serviceKey))
    }
  }

  def resource(orgKey: String, serviceKey: String, version: String, resourceKey: String) = Authenticated { request =>
    val service = getService(orgKey, serviceKey, Some(version))
    val resource = getResource(service, resourceKey)
    Ok(views.html.services.resource(request.user, service, resource))
  }

  def operation(orgKey: String, serviceKey: String, version: String, resourceKey: String, operationKey: String) = Authenticated { request =>
    val service = getService(orgKey, serviceKey, Some(version))
    val resource = getResource(service, resourceKey)
    val operation = Path.operation(resource, operationKey).getOrElse {
      sys.error(s"Resource[${service.key} ${resource.name}] does not have an operation matching [${operationKey}]")
    }

    Ok(views.html.services.operation(request.user, service, resource, operation))
  }

  def reference(orgKey: String, serviceKey: String, version: String, reference: String) = Authenticated { request =>
    val service = getService(orgKey, serviceKey, Some(version))
    val resourceKey = reference.split("\\.").headOption.getOrElse {
      sys.error(s"Invalid reference[$reference] - expected resource.field")
    }
    val resource = getResource(service, resourceKey)
    Redirect(Path.url(service, resource))
  }

  private def getOptionalService(orgKey: String, serviceKey: String, version: Option[String] = None): Option[Service] = {
    Organization.findByKey(orgKey).flatMap { org =>
      ServiceDao.findByOrganizationAndKey(org, serviceKey).flatMap { dao =>
        version match {
          case None => VersionDao.latestVersionForService(dao).map( v => Service(org, dao, v) )
          case Some("latest") => VersionDao.latestVersionForService(dao).map( v => Service(org, dao, v) )
          case Some(versionString: String) => VersionDao.findByServiceAndVersion(dao, versionString).map( v => Service(org, dao, v) )
        }
      }
    }
  }

  private def getService(orgKey: String, serviceKey: String, version: Option[String] = None): Service = {
    getOptionalService(orgKey, serviceKey, version).getOrElse {
      version match {
        case None => sys.error(s"OrgKey[$orgKey] Service[$serviceKey] does not have any versions")
        case Some(v: String) => sys.error(s"Service[$serviceKey] does not have a version[$v]")
      }
    }
  }

  private def getResource(service: Service, resourceKey: String): Resource = {
    service.resource(resourceKey).getOrElse {
      sys.error(s"Service[${service.key}] does not have a resource with key[$resourceKey]")
    }
  }

}
