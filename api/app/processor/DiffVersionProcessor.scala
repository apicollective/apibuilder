package processor

import cats.data.ValidatedNec
import cats.implicits._
import db._
import io.apibuilder.api.v0.models._
import io.apibuilder.task.v0.models.json._
import io.apibuilder.task.v0.models.{DiffVersionData, TaskType}
import lib.{AppConfig, ServiceDiff, Emails}
import play.twirl.api.Html

import java.util.UUID
import javax.inject.Inject

class DiffVersionProcessor @Inject()(
                              args: TaskProcessorArgs,
                              appConfig: AppConfig,
                              usersDao: UsersDao,
                              applicationsDao: ApplicationsDao,
                              organizationsDao: OrganizationsDao,
                              emails: Emails,
                              changesDao: ChangesDao,
                              versionsDao: VersionsDao,
                              watchesDao: WatchesDao,
) extends TaskProcessorWithData[DiffVersionData](args, TaskType.DiffVersion) {

  override def processRecord(id: String, data: DiffVersionData): ValidatedNec[String, Unit] = {
    diffVersion(data.oldVersionGuid, data.newVersionGuid)
    ().validNec
  }

  private[this] def diffVersion(oldVersionGuid: UUID, newVersionGuid: UUID): Unit = {
    versionsDao.findByGuid(Authorization.All, oldVersionGuid, isDeleted = None).foreach { oldVersion =>
      versionsDao.findByGuid(Authorization.All, newVersionGuid).foreach { newVersion =>
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

  private[this] def versionUpdated(
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
          org,
          application,
          version,
          breakingDiffs = breakingDiffs,
          nonBreakingDiffs = nonBreakingDiffs
        )
      }
    }
  }

  private[this] def versionUpdatedMaterialOnly(
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
          org,
          application,
          version,
          breakingDiffs = breakingDiffs,
          nonBreakingDiffs = nonBreakingDiffs
        )
      }
    }
  }

  private[this] def sendVersionUpsertedEmail(
                                              publication: Publication,
                                              version: Version,
                                              diffs: Seq[Diff],
                                            )(
                                              generateBody: (Organization, Application, Seq[Diff], Seq[Diff]) => Html,
                                            ): Unit = {
    val (breakingDiffs, nonBreakingDiffs) = diffs.partition {
      case _: DiffBreaking => true
      case _: DiffNonBreaking => false
      case _: DiffUndefinedType => true
    }

    applicationsDao.findAll(Authorization.All, version = Some(version), limit = 1).foreach { application =>
      organizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).foreach { org =>
        emails.deliver(
          context = Emails.Context.Application(application),
          org = org,
          publication = publication,
          subject = s"${org.name}/${application.name}:${version.version} Updated",
          body = generateBody(org, application, breakingDiffs, nonBreakingDiffs).toString
        ) { subscription =>
          watchesDao.findAll(
            Authorization.All,
            application = Some(application),
            userGuid = Some(subscription.user.guid),
            limit = 1
          ).nonEmpty
        }
      }
    }
  }

}

