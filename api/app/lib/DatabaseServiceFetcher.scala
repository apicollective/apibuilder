package lib

import core.ServiceFetcher
import db.{Authorization, VersionsDao}
import javax.inject.Inject

import io.apibuilder.spec.v0.models.Service

/**
  * Implements service fetch by querying the DB
  */
class DatabaseServiceFetcher @Inject() (
  versionsDao: VersionsDao
) {

  def instance(authorization: Authorization): ServiceFetcher = {
    new ServiceFetcher {
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
  }

}
