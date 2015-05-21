package actors

import com.gilt.apidoc.api.v0.models.{Application, ItemType, Organization}
import db.{ApplicationsDao, Authorization, ItemsDao, OrganizationsDao}
import java.util.UUID

object Search {

  def indexApplication(applicationGuid: UUID) {
    getInfo(applicationGuid) match {
      case Some((org, app)) => {
        val content = s"""${app.name} ${app.key} ${app.description.getOrElse("")}""".trim.toLowerCase
        ItemsDao.upsert(app.guid, ItemType.Application, app.name, app.description, content)
      }
      case None => {
        ItemsDao.delete(applicationGuid)
      }
    }
  }

  private def getInfo(applicationGuid: UUID): Option[(Organization, Application)] = {
    ApplicationsDao.findByGuid(Authorization.All, applicationGuid).flatMap { application =>
      OrganizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
        (org, application)
      }
    }
  }

}