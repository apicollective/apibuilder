package db

import io.flow.postgresql.Query
import play.api.Application
import play.api.db.Database

trait DbUtils {
  def app: Application

  def database: Database = app.injector.instanceOf[Database]

  def execute(queries: Query*): Unit = {
    database.withConnection { c =>
      queries.foreach(_.anormSql().executeUpdate()(c))
    }
  }

}
