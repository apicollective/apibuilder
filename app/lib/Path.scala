package lib

import db.{ Organization, ServiceDao }
import models.{ Operation, Resource, Service }

/**
 * Defines paths used in routing for services and operations and such
 */
object Path {

  def url(org: Organization): String = {
    s"/${org.key}"
  }

  def url(service: Service): String = {
    s"/${service.org.key}/docs/${service.key}/latest"
  }

  def url(serviceDao: ServiceDao): String = {
    s"/${serviceDao.org.key}/docs/${serviceDao.key}/latest"
  }

  def url(service: Service, version: String): String = {
    s"/${service.org.key}/docs/${service.key}/${UrlKey.generate(version)}"
  }

  def url(service: Service, resource: Resource): String = {
    url(service) + "/" + resource.key
  }

  def url(service: Service, resource: Resource, op: Operation): String = {
    url(service, resource) + "/" + OperationKey(op).key
  }


  def reference(service: Service, reference: String): String = {
    s"/${service.org.key}/references/${service.key}/${service.version.version}/${UrlKey.generate(reference)}"
  }

  def operation(resource: Resource, key: String): Option[Operation] = {
    resource.operations.find { op =>
      OperationKey(op).key == key
    }
  }

}
