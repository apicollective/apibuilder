package db

import io.apibuilder.internal.v0.models.{Task, TaskData, TaskDataDiffVersion, TaskDataUndefinedType}
import org.scalatest.{FunSpec, Matchers}
import org.postgresql.util.PSQLException
import java.util.UUID

import anorm._
import org.joda.time.DateTime
import play.api.db._
import play.api.Play.current

class TasksDaoSpec extends FunSpec with Matchers with util.TestApplication {

  private[this] def setDeletedAt(task: Task, days: Int) {
    val query = s"""
      update tasks set deleted_at = timezone('utc', now()) - interval '$days days' where guid = {guid}::uuid
    """

    DB.withConnection { implicit c =>
      SQL(query).on('guid -> task.guid).execute()
    }
  }

  lazy val user = Util.createRandomUser()

  private[this] def createTaskDataDiffVersion(
    oldGuid: UUID = UUID.randomUUID,
    newGuid: UUID = UUID.randomUUID,
    numberAttempts: Int = 0
  ): Task = {
    val guid = DB.withConnection { implicit c =>
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

  it("findByGuid") {
    val oldGuid = UUID.randomUUID
    val newGuid = UUID.randomUUID
    createTaskDataDiffVersion(oldGuid, newGuid).data should be(TaskDataDiffVersion(oldGuid, newGuid))
  }

  it("softDelete") {
    val task = createTaskDataDiffVersion()
    tasksDao.softDelete(user, task)
    tasksDao.findByGuid(task.guid) should be(None)
  }

  it("incrementNumberAttempts") {
    val task = createTaskDataDiffVersion()
    val original = task.numberAttempts
    tasksDao.incrementNumberAttempts(user, task)
    tasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.numberAttempts should be(original + 1)
  }

  it("recordError") {
    val task = createTaskDataDiffVersion()
    tasksDao.recordError(user, task, "Test")
    tasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.lastError should be(Some("Test"))
  }

  describe("findAll") {

    it("nOrFewerAttempts") {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts)
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts - 1)
      ).map(_.guid) should be(Nil)
    }

    it("nOrMoreAttempts") {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts)
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts + 1)
      ).map(_.guid) should be(Nil)
    }

    it("nOrMoreMinutesOld") {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrBefore = Some(DateTime.now.plusHours(1))
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrBefore = Some(DateTime.now.minusHours(1))
      ).map(_.guid) should be(Nil)
    }

    it("nOrMoreMinutesYoung") {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrAfter = Some(DateTime.now.minusHours(1))
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        createdOnOrAfter = Some(DateTime.now.plusHours(1))
      ).map(_.guid) should be(Nil)
    }

    it("isDeleted") {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) should be(Nil)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.softDelete(user, task)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) should be(Nil)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) should be(Seq(task.guid))

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) should be(Seq(task.guid))

    }

    it("deletedAtLeastNDaysAgo") {
      val task = createTaskDataDiffVersion()

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(0)
      ) should be(Nil)

      tasksDao.softDelete(user, task)

      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ).map(_.guid) should be(Nil)

      setDeletedAt(task, 89)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ) should be(Nil)

      setDeletedAt(task, 91)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None,
        deletedAtLeastNDaysAgo = Some(90)
      ).map(_.guid) should be(Seq(task.guid))
    }
  }

  describe("purge") {

    it("raises error if recently deleted") {
      val task = createTaskDataDiffVersion()
      tasksDao.softDelete(user, task)
      val ex = intercept[PSQLException] {
        tasksDao.purge(user, task)
      }
      println(ex.getMessage)
      ex.getMessage.contains("ERROR: Physical deletes on this table can occur only after 1 month of deleting the records") should be(true)
    }

    it("purges if old") {
      val task = createTaskDataDiffVersion()
      tasksDao.softDelete(user, task)
      setDeletedAt(task, 45)
      tasksDao.purge(user, task)
      tasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ) should be(Nil)
    }

  }

}
