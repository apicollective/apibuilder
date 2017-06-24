package core

import io.apibuilder.spec.v0.models.Service

trait ServiceFetcher {

  def fetch(uri: String): Service

}

case class MockServiceFetcher() extends ServiceFetcher {

  val services = scala.collection.mutable.Map[String, Service]()

  def add(uri: String, service: Service) {
    services += (uri -> service)
  }

  override def fetch(uri: String): Service = {
    services.get(uri).getOrElse {
      sys.error(s"No mock found for imported service w/ uri[$uri]")
    }
  }

}
