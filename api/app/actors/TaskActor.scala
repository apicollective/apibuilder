package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import com.bryzek.apidoc.api.v0.models.{Application, Diff, DiffBreaking, DiffNonBreaking, DiffUndefinedType, Publication, Version}
import com.bryzek.apidoc.internal.v0.models.{Task, TaskDataDiffVersion, TaskDataIndexApplication, TaskDataUndefinedType}
import db.{ApplicationsDao, Authorization, ChangesDao, OrganizationsDao, TasksDao, UsersDao, VersionsDao}
import lib.{ServiceDiff, Text}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current
import java.util.UUID
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
  applicationsDao: ApplicationsDao,
  changesDao: ChangesDao,
  emails: Emails,
  organizationsDao: OrganizationsDao,
  search: Search,
  tasksDao: TasksDao,
  usersDao: UsersDao,
  versionsDao: VersionsDao
) extends Actor with ActorLogging with ErrorHandler {

  implicit val ec = system.dispatchers.lookup("task-actor-context")

  private[this] val NumberDaysBeforePurge = 30
  private[this] case class Process(guid: UUID)

  system.scheduler.schedule(1.hour, 1.hour, self, TaskActor.Messages.RestartDroppedTasks)
  system.scheduler.schedule(1.day, 1.day, self, TaskActor.Messages.NotifyFailed)
  system.scheduler.schedule(1.day, 1.day, self, TaskActor.Messages.PurgeOldTasks)
  
  def receive = {

    case m @ TaskActor.Messages.Created(guid) => withVerboseErrorHandler(m) {
      self ! Process(guid)
    }

    case m @ Process(guid) => withVerboseErrorHandler(m) {
      tasksDao.findByGuid(guid).map { task =>
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
        nOrMoreMinutesOld = Some(1)
      ).foreach { task =>
        self ! Process(task.guid)
      }
    }

    case m @ TaskActor.Messages.NotifyFailed => withVerboseErrorHandler(m) {
      val errors = tasksDao.findAll(
        nOrMoreAttempts = Some(2)
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
        tasksDao.purge(usersDao.AdminUser, task)
      }
    }

    case m: Any => logUnhandledMessage(m)
  }

  private[this] def diffVersion(oldVersionGuid: UUID, newVersionGuid: UUID) {
    versionsDao.findByGuid(Authorization.All, oldVersionGuid, isDeleted = None).map { oldVersion =>
      versionsDao.findByGuid(Authorization.All, newVersionGuid).map { newVersion =>
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
          }
        }
      }
    }
  }

  private[this] def versionUpdated(
    version: Version,
    diffs: Seq[Diff]
  ) {
    // Only send email if something has actually changed
    if (!diffs.isEmpty) {
      val breakingDiffs = diffs.flatMap { d =>
        d match {
          case d: DiffBreaking => Some(d.description)
          case d: DiffNonBreaking => None
          case d: DiffUndefinedType => Some(d.description)
        }
      }

      val nonBreakingDiffs = diffs.flatMap { d =>
        d match {
          case d: DiffBreaking => None
          case d: DiffNonBreaking => Some(d.description)
          case d: DiffUndefinedType => None
        }
      }

      applicationsDao.findAll(Authorization.All, version = Some(version), limit = 1).headOption.map { application =>
        organizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
          emails.deliver(
            context = Emails.Context.Application(application),
            org = org,
            publication = Publication.VersionsCreate,
            subject = s"${org.name}/${application.name}:${version.version} Uploaded",
            body = views.html.emails.versionCreated(
              org,
              application,
              version,
              breakingDiffs = breakingDiffs,
              nonBreakingDiffs = nonBreakingDiffs
            ).toString
          )
        }
      }
    }
  }

  def processTask[T](task: Task, attempt: Try[T]) {
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
