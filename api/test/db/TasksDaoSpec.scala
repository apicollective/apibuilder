package db

import com.gilt.apidoc.internal.v0.models.{Task, TaskData, TaskDataDiffVersion, TaskDataIndexVersion, TaskDataUndefinedType}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import play.api.db._
import play.api.Play.current

class TasksDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  lazy val user = Util.createRandomUser()

  private def createTaskDataDiffVersion(
    oldGuid: UUID = UUID.randomUUID,
    newGuid: UUID = UUID.randomUUID,
    numberAttempts: Int = 0
  ): Task = {
    val guid = DB.withConnection { implicit c =>
      TasksDao.insert(c, user, TaskDataDiffVersion(oldGuid, newGuid))
    }

    val task = TasksDao.findByGuid(guid).getOrElse {
      sys.error("failed to find task")
    }

    (0 to numberAttempts).foreach { _ =>
      TasksDao.incrementNumberAttempts(user, task)
    }

    TasksDao.findByGuid(guid).getOrElse {
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
    TasksDao.softDelete(user, task)
    TasksDao.findByGuid(task.guid) should be(None)
  }

  it("incrementNumberAttempts") {
    val task = createTaskDataDiffVersion()
    val original = task.numberAttempts
    TasksDao.incrementNumberAttempts(user, task)
    TasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.numberAttempts should be(original + 1)
  }

  it("recordError") {
    val task = createTaskDataDiffVersion()
    TasksDao.recordError(user, task, "Test")
    TasksDao.findByGuid(task.guid).getOrElse {
      sys.error("failed to find task")
    }.lastError should be(Some("Test"))
  }

  describe("findAll") {

    it("nOrFewerAttempts") {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts)
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrFewerAttempts = Some(task.numberAttempts - 1)
      ).map(_.guid) should be(Nil)
    }

    it("nOrMoreAttempts") {
      val task = createTaskDataDiffVersion(numberAttempts = 2)

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts)
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreAttempts = Some(task.numberAttempts + 1)
      ).map(_.guid) should be(Nil)
    }

    it("nOrMoreMinutesOld") {
      val task = createTaskDataDiffVersion()

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreMinutesOld = Some(0)
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.findAll(
        guid = Some(task.guid),
        nOrMoreMinutesOld = Some(10)
      ).map(_.guid) should be(Nil)
    }

    it("isDeleted") {
      val task = createTaskDataDiffVersion()

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) should be(Nil)

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.softDelete(user, task)

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(false)
      ).map(_.guid) should be(Nil)

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = Some(true)
      ).map(_.guid) should be(Seq(task.guid))

      TasksDao.findAll(
        guid = Some(task.guid),
        isDeleted = None
      ).map(_.guid) should be(Seq(task.guid))

    }
  }

}
