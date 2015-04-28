package lib

import core.ServiceFetcher
import com.gilt.apidoc.spec.v0.models.Service
import db.{ApplicationsDao, Authorization, VersionsDao}
import java.util.UUID

/**
  * Implements service fetch by querying the DB
  */
case class DatabaseServiceFetcher(authorization: Authorization) extends ServiceFetcher {

  override def fetch(uri: String): Service = {
    println(s"fetch uri[$uri]")
    ServiceUri.parse(uri) match {

      case None => {
        sys.error(s"could not parse URI[$uri]")
      }

      case Some(serviceUri) => {
        VersionsDao.findVersion(authorization, serviceUri.org, serviceUri.app, serviceUri.version).headOption.map(_.service).getOrElse {
          sys.error(s"Error while fetching service for URI[$uri] - could not find [${serviceUri.org}/${serviceUri.app}:${serviceUri.version}]")
        }
      }
    }
  }

}
