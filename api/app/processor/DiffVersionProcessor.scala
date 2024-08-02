package processor

import cats.data.ValidatedNec
import cats.implicits._
import db._
import io.apibuilder.api.v0.models._
import io.apibuilder.task.v0.models.json._
import io.apibuilder.task.v0.models.{DiffVersionData, TaskType}
import lib.{AppConfig, Emails, ServiceDiff}
import models.{OrganizationsModel, VersionsModel}
import play.twirl.api.Html

import java.util.UUID
import javax.inject.Inject

class DiffVersionProcessor @Inject()(
                                      args: TaskProcessorArgs,
                                      appConfig: AppConfig,
                                      usersDao: InternalUsersDao,
                                      applicationsDao: InternalApplicationsDao,
                                      organizationsDao: InternalOrganizationsDao,
                                      emails: Emails,
                                      changesDao: InternalChangesDao,
                                      versionsDao: VersionsDao,
                                      versionsModel: VersionsModel,
                                      watchesDao: InternalWatchesDao,
                                      orgModel: OrganizationsModel
) extends TaskProcessorWithData[DiffVersionData](args, TaskType.DiffVersion) {

  override def processRecord(id: String, data: DiffVersionData): ValidatedNec[String, Unit] = {
    diffVersion(data.oldVersionGuid, data.newVersionGuid)
    ().validNec
  }

  private def diffVersion(oldVersionGuid: UUID, newVersionGuid: UUID): Unit = {
    versionsDao.findByGuid(Authorization.All, oldVersionGuid, isDeleted = None)
      .flatMap(versionsModel.toModel)
      .foreach { oldVersion =>
      versionsDao.findByGuid(Authorization.All, newVersionGuid)
        .flatMap(versionsModel.toModel)
        .foreach { newVersion =>
        ServiceDiff(oldVersion.service, newVersion.service).differences match {
          case Nil => {
            // No-op
          }
          case diffs => {
            changesDao.upsert(
              createdBy = usersDao.AdminUser,
              fromVersion = oldVersion,
              toVersion = newVersion,
              differences = diffs
            )
            versionUpdated(newVersion, diffs)
            versionUpdatedMaterialOnly(newVersion, diffs)
          }
        }
      }
    }
  }

  private def versionUpdated(
                                    version: Version,
                                    diffs: Seq[Diff],
                                  ): Unit = {
    if (diffs.nonEmpty) {
      sendVersionUpsertedEmail(
        publication = Publication.VersionsCreate,
        version = version,
        diffs = diffs,
      ) { (org, application, breakingDiffs, nonBreakingDiffs) =>
        views.html.emails.versionUpserted(
          appConfig,
          orgModel.toModel(org),
          application,
          version,
          breakingDiffs = breakingDiffs,
          nonBreakingDiffs = nonBreakingDiffs
        )
      }
    }
  }

  private def versionUpdatedMaterialOnly(
                                                version: Version,
                                                diffs: Seq[Diff],
                                              ): Unit = {
    val filtered = diffs.filter(_.isMaterial)
    if (filtered.nonEmpty) {
      sendVersionUpsertedEmail(
        publication = Publication.VersionsMaterialChange,
        version = version,
        diffs = filtered,
      ) { (org, application, breakingDiffs, nonBreakingDiffs) =>
        views.html.emails.versionUpserted(
          appConfig,
          orgModel.toModel(org),
          application,
          version,
          breakingDiffs = breakingDiffs,
          nonBreakingDiffs = nonBreakingDiffs
        )
      }
    }
  }

  private def sendVersionUpsertedEmail(
                                              publication: Publication,
                                              version: Version,
                                              diffs: Seq[Diff],
                                            )(
                                              generateBody: (InternalOrganization, InternalApplication, Seq[Diff], Seq[Diff]) => Html,
                                            ): Unit = {
    val (breakingDiffs, nonBreakingDiffs) = diffs.partition {
      case _: DiffBreaking => true
      case _: DiffNonBreaking => false
      case _: DiffUndefinedType => true
    }

    applicationsDao.findAll(Authorization.All, version = Some(version), limit = Some(1)).foreach { application =>
      organizationsDao.findAll(Authorization.All, applicationGuid = Some(application.guid), limit = Some(1)).foreach { org =>
        emails.deliver(
          context = Emails.Context.Application(application),
          org = orgModel.toModel(org),
          publication = publication,
          subject = s"${org.name}/${application.name}:${version.version} Updated",
          body = generateBody(org, application, breakingDiffs, nonBreakingDiffs).toString
        ) { subscription =>
          watchesDao.findAll(
            Authorization.All,
            applicationGuid = Some(application.guid),
            userGuid = Some(subscription.user.guid),
            limit = Some(1)
          ).nonEmpty
        }
      }
    }
  }

}

