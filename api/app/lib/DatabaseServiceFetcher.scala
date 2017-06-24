package lib

import core.ServiceFetcher
import io.apibuilder.spec.v0.models.Service
import db.{Authorization, VersionsDao}

/**
  * Implements service fetch by querying the DB
  */
case class DatabaseServiceFetcher(authorization: Authorization) extends ServiceFetcher {

  private[this] def versionsDao = play.api.Play.current.injector.instanceOf[VersionsDao]

  override def fetch(uri: String): Service = {
    ServiceUri.parse(uri) match {

      case None => {
        sys.error(s"could not parse URI[$uri]")
      }

      case Some(serviceUri) => {
        versionsDao.findVersion(authorization, serviceUri.org, serviceUri.app, serviceUri.version).headOption.map(_.service).getOrElse {
          sys.error(s"Error while fetching service for URI[$uri] - could not find [${serviceUri.org}/${serviceUri.app}:${serviceUri.version}]")
        }
      }
    }
  }

}
