package db

import com.typesafe.config.Config
import javax.sql.DataSource
import play.api.Environment
import play.api.db.DefaultDatabase

class TestDatabase(name: String, configuration: Config, environment: Environment)
  extends DefaultDatabase(name, configuration, environment) {
  override def createDataSource(): DataSource = ???

  override def closeDataSource(dataSource: DataSource): Unit = ???
}

