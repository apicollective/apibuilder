package util

import db.{Authorization, VersionsDao}
import io.apibuilder.api.v0.models.Version
import io.apibuilder.spec.v0.models.Service
import javax.inject.Inject

class ApibuilderServiceImportResolver @Inject() (
  versionsDao: VersionsDao,
) {

  /**
   * Expands each import into its service definition
   **/
  def resolve(auth: Authorization, service: Service): Seq[Service] = {
    resolve(auth, service, Map.empty).values.toSeq
  }

  private[this] def resolve(auth: Authorization, service: Service, acc: Map[String, Service]): Map[String, Service] = {
    println(s"Service ${service.name}: ${service.imports.map(_.namespace)}")
    service.imports.foldLeft(acc) { case (acc, imp) =>
      if (acc.contains(imp.namespace)) {
        acc
      } else {
        versionsDao.findVersion(auth, imp.organization.key, imp.application.key, imp.version) match {
          case None => {
            println(s"Could not resolve import: ${imp.organization.key}/${imp.application.key}/${imp.version}")
            acc
          }
          case Some(v: Version) => resolve(auth, v.service, acc + (imp.namespace -> v.service))
        }
      }
    }
  }
}
