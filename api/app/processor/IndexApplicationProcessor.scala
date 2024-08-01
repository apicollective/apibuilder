package processor

import cats.data.ValidatedNec
import cats.implicits._
import db._
import io.apibuilder.api.v0.models.ApplicationSummary
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.task.v0.models.TaskType

import java.util.UUID
import javax.inject.Inject


class IndexApplicationProcessor @Inject()(
                                           args: TaskProcessorArgs,
                                           applicationsDao: InternalApplicationsDao,
                                           itemsDao: ItemsDao,
                                           organizationsDao: InternalOrganizationsDao
) extends TaskProcessorWithGuid(args, TaskType.IndexApplication) {

  override def processRecord(applicationGuid: UUID): ValidatedNec[String, Unit] = {
    getInfo(applicationGuid) match {
      case Some((org, app)) => {
        val content = s"""${app.name} ${app.key} ${app.description.getOrElse("")}""".trim.toLowerCase
        itemsDao.upsert(
          guid = app.guid,
          detail = ApplicationSummary(
            guid = app.guid,
            organization = Reference(guid = org.guid, key = org.key),
            key = app.key
          ),
          label = s"${org.key}/${app.key}",
          description = app.description,
          content = content
        )
      }
      case None => {
        itemsDao.delete(applicationGuid)
      }
    }
    ().validNec
  }

  private def getInfo(applicationGuid: UUID): Option[(InternalOrganization, InternalApplication)] = {
    applicationsDao.findByGuid(Authorization.All, applicationGuid).flatMap { application =>
      organizationsDao.findAll(Authorization.All, applicationGuid = Some(application.guid), limit = Some(1)).headOption.map { org =>
        (org, application)
      }
    }
  }
}