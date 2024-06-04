package db

import java.util.UUID

import anorm._
import io.apibuilder.api.v0.models.User
import io.apibuilder.internal.v0.models.{Task, TaskDataDiffVersion}
import org.joda.time.DateTime
import org.postgresql.util.PSQLException
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db._

class TasksDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private[this] def setDeletedAt(task: Task, days: Int): Unit = {
    val query = s"""
      update tasks set deleted_at = timezone('utc', now()) - interval '$days days' where guid = {guid}::uuid
    """

    injector.instanceOf[DBApi].database("default").withConnection { implicit c =>
      SQL(query).on("guid" -> task.guid).execute()
    }
  }

  private[this] lazy val user: User = createRandomUser()

  private[this] def createTaskDataDiffVersion(
    oldGuid: UUID = UUID.randomUUID,
    newGuid: UUID = UUID.randomUUID,
    numberAttempts: Int = 0
  ): Task = {
    val guid = injector.instanceOf[DBApi].database("default").withConnection { implicit c =>
      tasksDao.insert(c, user, TaskDataDiffVersion(oldGuid, newGuid))
    }

    val task = tasksDao.findByGuid(guid).getOrElse {
      sys.error("failed to find task")
    }

    (0 to numberAttempts).foreach { _ =>
      tasksDao.incrementNumberAttempts(user, task)
    }

    tasksDao.findByGuid(guid).getOrElse {
      sys.error("failed to create task")
    }
  }

  "findByGuid" in {
    val oldGuid = UUID.randomUUID
    val newGuid = UUID.randomUUID
    createTaskDataDiffVersion(oldGuid, newGuid).data must be(TaskDataDiffVersion(oldGuid, newGuid))
  }

  "softDelete" in {
    val task = createTaskDataDiffVersion()
    tasksDao.softDelete(user, task)
    tasksDao.findByGuid(task.guid) must be(None)
  }

  "incrementNumberAttempts" in {
    val task = createTaskDataDiffVersion()
    val original = task.numberAttempts
    tasksDao.incrementNumberAttempts(user, task)
    tasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.numberAttempts must be(original + 1)
  }

  "recordError" in {
    val task = createTaskDataDiffVersion()
    tasksDao.recordError(user, task, "Test")
    tasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.lastError must be(Some("Test"))
  }

  "findAll" must {

    "nOrFewerAttempts" in {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts)
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts - 1)
      ).map(_.guid) must be(Nil)
    }

    "nOrMoreAttempts" in {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts)
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts + 1)
      ).map(_.guid) must be(Nil)
    }

    "nOrMoreMinutesOld" in {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrBefore = Some(DateTime.now.plusHours(1))
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrBefore = Some(DateTime.now.minusHours(1))
      ).map(_.guid) must be(Nil)
    }

    "nOrMoreMinutesYoung" in {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrAfter = Some(DateTime.now.minusHours(1))
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrAfter = Some(DateTime.now.plusHours(1))
      ).map(_.guid) must be(Nil)
    }

    "isDeleted" in {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) must be(Nil)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.softDelete(user, task)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) must be(Nil)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) must be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) must be(Seq(task.guid))

    }

    "deletedAtLeastNDaysAgo" in {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(0)
      ) must be(Nil)

      tasksDao.softDelete(user, task)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ).map(_.guid) must be(Nil)

      setDeletedAt(task, 89)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ) must be(Nil)

      setDeletedAt(task, 91)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ).map(_.guid) must be(Seq(task.guid))
    }
  }

  "purge" must {

    "raises error if recently deleted" in {
      val task = createTaskDataDiffVersion()
      tasksDao.softDelete(user, task)
      val ex = intercept[PSQLException] {
        tasksDao.purge(task)
      }
      println(ex.getMessage)
      ex.getMessage.contains("ERROR: Physical deletes on this table can occur only after 1 month of deleting the records") must be(true)
    }

    "purges if old" in {
      val task = createTaskDataDiffVersion()
      tasksDao.softDelete(user, task)
      setDeletedAt(task, 45)
      tasksDao.purge(task)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ) must be(Nil)
    }

  }

}
