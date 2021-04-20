package core

import io.apibuilder.spec.v0.models.Service

import scala.collection.mutable

trait ServiceFetcher {

  def fetch(uri: String): Service

}

case class MockServiceFetcher() extends ServiceFetcher {

  val services: mutable.Map[String, Service] = scala.collection.mutable.Map[String, Service]()

  def add(uri: String, service: Service): Unit = {
    services += (uri -> service)
  }

  override def fetch(uri: String): Service = {
    services.getOrElse(uri, sys.error(s"No mock found for imported service w/ uri[$uri]"))
  }

}
