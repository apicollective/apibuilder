package actors

import lib.ServiceDiff
import com.gilt.apidoc.api.v0.models.{Publication, Version}
import com.gilt.apidoc.internal.v0.models.{Difference, DifferenceBreaking, DifferenceNonBreaking, DifferenceUndefinedType}
import com.gilt.apidoc.internal.v0.models.{Task, TaskDataDiffVersion, TaskDataIndexVersion, TaskDataUndefinedType}
import db.{ApplicationsDao, Authorization, ChangesDao, OrganizationsDao, TasksDao, UsersDao, VersionsDao}
import play.api.Logger
import akka.actor.Actor
import java.util.UUID

object TaskActor {

  object Messages {
    case class TaskCreated(guid: UUID)
  }

}

class TaskActor extends Actor {

  def receive = {

    case TaskActor.Messages.TaskCreated(guid) => Util.withVerboseErrorHandler(
      s"TaskActor.Messages.TaskCreated($guid)", {
        TasksDao.findByGuid(guid).map { task =>
          TasksDao.incrementNumberAttempts(UsersDao.AdminUser, task)
          println("TASK: " + task)

          task.data match {
            case TaskDataDiffVersion(oldVersionGuid, newVersionGuid) => {
              println(" - TaskDataDiffVersion")
              VersionsDao.findByGuid(Authorization.All, oldVersionGuid, isDeleted = None).map { oldVersion =>
                VersionsDao.findByGuid(Authorization.All, newVersionGuid, isDeleted = None).map { newVersion =>
                  val diff = ServiceDiff(oldVersion.service, newVersion.service).differences
                  ChangesDao.upsert(UsersDao.AdminUser, oldVersion, newVersion, diff)
                  versionUpdated(oldVersion, newVersion, diff)
                }
              }
            }

            case TaskDataIndexVersion(versionGuid) => {
              println(" - TaskDataIndexVersion")
            }

            case TaskDataUndefinedType(desc) => {
              TasksDao.recordError(UsersDao.AdminUser, task, "Task actor got an undefined data type: " + desc)
            }
          }
        }
      }
    )

    case m: Any => {
      Logger.error("Task actor got an unhandled message: " + m)
    }

  }


  private def versionUpdated(
    oldVersion: Version,
    newVersion: Version,
    diff: Seq[Difference]
  ) {
    ApplicationsDao.findAll(Authorization.All, version = Some(newVersion), limit = 1).headOption.map { application =>
      OrganizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
        Emails.deliver(
          org = org,
          publication = Publication.VersionsCreate,
          subject = s"${org.name}/${application.name}: New Version Uploaded (${newVersion.version}) ",
          body = views.html.emails.versionCreated(
            org,
            application,
            newVersion,
            breakingChanges = diff.filter { d =>
              d match {
                case DifferenceBreaking(desc) => true
                case DifferenceNonBreaking(desc) => false
                case DifferenceUndefinedType(desc) => false
              }
            }.map(_.asInstanceOf[DifferenceBreaking]),
            nonBreakingChanges = diff.filter { d =>
              d match {
                case DifferenceBreaking(desc) => false
                case DifferenceNonBreaking(desc) => true
                case DifferenceUndefinedType(desc) => false
              }
            }.map(_.asInstanceOf[DifferenceNonBreaking])
          ).toString
        )
      }
    }
  }

}

