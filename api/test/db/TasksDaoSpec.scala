package db

import com.gilt.apidoc.internal.v0.models.{Task, TaskData, TaskDataDiffVersion, TaskDataIndexVersion, TaskDataUndefinedType}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID
import play.api.db._
import play.api.Play.current

class TasksDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  lazy val user = Util.createRandomUser()

  it("findByGuid") {
    val oldGuid = UUID.randomUUID
    val newGuid = UUID.randomUUID

    val guid = DB.withConnection { implicit c =>
      TasksDao.insert(c, user, TaskDataDiffVersion(oldGuid, newGuid))
    }

    val task = TasksDao.findByGuid(guid).getOrElse {
      sys.error("failed to create task")
    }
    task should be(Task(guid, TaskDataDiffVersion(oldGuid, newGuid), task.numberAttempts, None))
  }

}
