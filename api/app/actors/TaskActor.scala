package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import io.apibuilder.api.v0.models.{Application, Diff, DiffBreaking, DiffNonBreaking, DiffUndefinedType, Organization, Publication, Version}
import io.apibuilder.internal.v0.models.{Task, TaskDataDiffVersion, TaskDataIndexApplication, TaskDataUndefinedType}
import db.{ApplicationsDao, Authorization, ChangesDao, OrganizationsDao, TasksDao, UsersDao, VersionsDao, WatchesDao}
import lib.{AppConfig, ServiceDiff, Text}

import java.util.UUID
import org.joda.time.DateTime
import play.twirl.api.Html

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object TaskActor {

  object Messages {
    case class Created(guid: UUID)
    case object RestartDroppedTasks
    case object PurgeOldTasks
    case object NotifyFailed
  }

}

@javax.inject.Singleton
class TaskActor @javax.inject.Inject() (
  system: ActorSystem,
  appConfig: AppConfig,
  applicationsDao: ApplicationsDao,
  changesDao: ChangesDao,
  emails: Emails,
  organizationsDao: OrganizationsDao,
  search: Search,
  tasksDao: TasksDao,
  usersDao: UsersDao,
  versionsDao: VersionsDao,
  watchesDao: WatchesDao
) extends Actor with ActorLogging with ErrorHandler {

  private[this] implicit val ec: ExecutionContext = system.dispatchers.lookup("task-actor-context")

  private[this] val NumberDaysBeforePurge = 30
  private[this] case class Process(guid: UUID)

  system.scheduler.scheduleAtFixedRate(1.hour, 1.hour, self, TaskActor.Messages.RestartDroppedTasks)
  system.scheduler.scheduleAtFixedRate(1.day, 1.day, self, TaskActor.Messages.NotifyFailed)
  system.scheduler.scheduleAtFixedRate(1.day, 1.day, self, TaskActor.Messages.PurgeOldTasks)
  
  def receive: Receive = {

    case m @ TaskActor.Messages.Created(guid) => withVerboseErrorHandler(m) {
      self ! Process(guid)
    }

    case m @ Process(guid) => withVerboseErrorHandler(m) {
      tasksDao.findByGuid(guid).foreach { task =>
        tasksDao.incrementNumberAttempts(usersDao.AdminUser, task)

        task.data match {
          case TaskDataDiffVersion(oldVersionGuid, newVersionGuid) => {
            processTask(task, Try(diffVersion(oldVersionGuid, newVersionGuid)))
          }

          case TaskDataIndexApplication(applicationGuid) => {
            processTask(task, Try(search.indexApplication(applicationGuid)))
          }

          case TaskDataUndefinedType(desc) => {
            tasksDao.recordError(usersDao.AdminUser, task, "Task actor got an undefined data type: " + desc)
          }
        }
      }
    }

    case m @ TaskActor.Messages.RestartDroppedTasks => withVerboseErrorHandler(m) {
      tasksDao.findAll(
        nOrFewerAttempts = Some(2),
        createdOnOrBefore = Some(DateTime.now.minusMinutes(1))
      ).foreach { task =>
        self ! Process(task.guid)
      }
    }

    case m @ TaskActor.Messages.NotifyFailed => withVerboseErrorHandler(m) {
      val errors = tasksDao.findAll(
        nOrMoreAttempts = Some(2),
        isDeleted = Some(false),
        createdOnOrAfter = Some(DateTime.now.minusDays(3))
      ).map { task =>
        val errorType = task.data match {
          case TaskDataDiffVersion(a, b) => s"TaskDataDiffVersion($a, $b)"
          case TaskDataIndexApplication(guid) => s"TaskDataIndexApplication($guid)"
          case TaskDataUndefinedType(desc) => s"TaskDataUndefinedType($desc)"
        }

        val errorMsg = Text.truncate(task.lastError.getOrElse("No information on error"), 500)
        s"$errorType task ${task.guid}: $errorMsg"
      }
      emails.sendErrors(
        subject = "One or more tasks failed",
        errors = errors
      )
    }

    case m @ TaskActor.Messages.PurgeOldTasks => withVerboseErrorHandler(m) {
      tasksDao.findAll(
        isDeleted = Some(true),
        deletedAtLeastNDaysAgo = Some(NumberDaysBeforePurge)
      ).foreach { task =>
        tasksDao.purge(task)
      }
    }

    case m: Any => logUnhandledMessage(m)
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

  def processTask[T](task: Task, attempt: Try[T]): Unit = {
    attempt match {
      case Success(_) => {
        tasksDao.softDelete(usersDao.AdminUser, task)
      }
      case Failure(ex) => {
        tasksDao.recordError(usersDao.AdminUser, task, ex)
      }
    }
  }

}
