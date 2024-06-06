package processor

import anorm.SqlParser
import io.flow.postgresql.Query
import play.api.db.Database

import javax.inject.Inject

class TaskDispatchActorCompanion @Inject() (
  database: Database
) {
  private[this] val TypesQuery = Query(
    "select distinct type from tasks where next_attempt_at <= now()"
  )

  def typesWithWork: Seq[TaskType] = {
    database
      .withConnection { c =>
        TypesQuery.as(SqlParser.str(1).*)(c)
      }
      .map(TaskType.fromString).flatMap(_.toOption)
  }

}
