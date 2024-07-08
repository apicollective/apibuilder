package db

private[db] object AuditsDao {

  def query(tableName: String): String = {
    Seq(
      queryCreation(tableName),
      s"${tableName}.updated_at",
      s"${tableName}.updated_by_guid"
    ).mkString(",\n    ")
  }

  private def queryCreation(tableName: String): String = {
    Seq(
      s"${tableName}.created_at",
      s"${tableName}.created_by_guid"
    ).mkString(",\n    ")
  }

  def queryCreationDefaultingUpdatedAt(tableName: String): String = {
    Seq(
      s"${tableName}.created_at",
      s"${tableName}.created_by_guid",
      s"${tableName}.created_at as updated_at",
      s"${tableName}.created_by_guid as updated_by_guid"
    ).mkString(",\n    ")
  }

}
