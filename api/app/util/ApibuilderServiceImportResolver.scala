package util

import db.{Authorization, VersionsDao}
import io.apibuilder.api.v0.models.Version
import io.apibuilder.spec.v0.models.Service

object ApibuilderServiceImportResolver {

  def resolveChildren(service: Service, versionsDao: VersionsDao, auth: Authorization): Map[String, Service] = {

    def resolve(service: Service, acc: Map[String, Service] = Map.empty): Map[String, Service] = {
      service.imports.foldLeft(acc) { case (acc, imp) =>
        if (acc.contains(imp.namespace)) {
          acc
        } else {
          versionsDao.findVersion(auth, imp.organization.key, imp.application.key, imp.version) match {
            case None => acc
            case Some(v: Version) => resolve(v.service, acc + (imp.namespace -> v.service))
          }
        }
      }
    }
    
    resolve(service)
  }
  
}
