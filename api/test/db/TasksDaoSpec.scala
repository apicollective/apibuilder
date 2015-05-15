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
    newGuid: UUID = UUID.randomUUID
  ): Task = {
    val guid = DB.withConnection { implicit c =>
      TasksDao.insert(c, user, TaskDataDiffVersion(oldGuid, newGuid))
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

}
