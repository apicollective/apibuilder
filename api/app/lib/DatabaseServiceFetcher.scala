package lib

import core.ServiceFetcher
import db.{Authorization, VersionsDao}

import javax.inject.Inject
import io.apibuilder.spec.v0.models.Service
import models.VersionsModel

/**
  * Implements service fetch by querying the DB
  */
class DatabaseServiceFetcher @Inject() (
  versionsDao: VersionsDao,
  versionsModel: VersionsModel,
) {

  def instance(authorization: Authorization): ServiceFetcher = {
    new ServiceFetcher {
      override def fetch(uri: String): Service = {
        val serviceUri = ServiceUri.parse(uri).getOrElse {
          sys.error(s"could not parse URI[$uri]")
        }

        versionsDao.findVersion(authorization, serviceUri.org, serviceUri.app, serviceUri.version)
          .flatMap(versionsModel.toModel)
          .map(_.service).getOrElse {
          sys.error(s"Error while fetching service for URI[$serviceUri] - could not find [${serviceUri.org}/${serviceUri.app}:${serviceUri.version}]")
        }
      }
    }
  }

}
