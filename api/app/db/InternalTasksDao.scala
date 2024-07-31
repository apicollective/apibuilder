package db

import db.generated.{Task, TaskForm}
import io.apibuilder.task.v0.models.TaskType
import lib.Constants
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

class InternalTasksDao @Inject() (
  dao: generated.TasksDao
) {
  def findByTypeIdAndType(typeId: String, typ: TaskType): Option[Task] = {
    dao.findByTypeIdAndType(typeId, typ.toString)
  }

  // TODO: Use a bulk insert for this method
  def queueBatch(typ: TaskType, ids: Seq[String]): Unit = {
    ids.foreach { id =>
      queue(typ, id)
    }
  }

  def queue(typ: TaskType, id: String, organizationGuid: Option[UUID] = None, data: JsValue = Json.obj()): Unit = {
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
    if (dao.findByTypeIdAndTypeWithConnection(c, (id, typ.toString)).isEmpty) {
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
