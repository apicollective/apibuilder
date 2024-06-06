package db

import db.generated.{Task, TaskForm}
import lib.Constants
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import processor.TaskType

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

class InternalTasksDao @Inject() (
  dao: generated.TasksDao
) {
  def findByTypeIdAndType(typeId: String, typ: TaskType): Option[Task] = {
    dao.findByTypeIdAndType(typeId, typ.toString)
  }

  def queue(typ: TaskType, id: String, organizationGuid: Option[UUID], data: JsValue = Json.obj()): Unit = {
    dao.db.withConnection { c =>
      queueWithConnection(c, typ, id, organizationGuid = organizationGuid, data = data)
    }
  }

  def queueWithConnection(
    c: Connection,
    typ: TaskType,
    id: String,
    organizationGuid: Option[UUID] = None,
    data: JsValue = Json.obj()
  ): Unit = {
    if (dao.findByTypeIdAndTypeWithConnection(c, id, typ.toString).isEmpty) {
      dao.upsertByTypeIdAndType(
        c,
        Constants.DefaultUserGuid,
        TaskForm(
          id = s"$typ:$id",
          `type` = typ.toString,
          typeId = id,
          organizationGuid = organizationGuid,
          data = data,
          numAttempts = 0,
          nextAttemptAt = DateTime.now,
          errors = None,
          stacktrace = None
        )
      )
    }
  }

}
