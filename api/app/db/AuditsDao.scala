package db

private[db] object AuditsDao {

  def query(tableName: String): String = {
    Seq(
      queryCreation(tableName),
      s"${tableName}.updated_at as audit_updated_at",
      s"${tableName}.updated_by_guid as audit_updated_by_guid"
    ).mkString(",\n    ")
  }

  def queryCreation(tableName: String): String = {
    Seq(
      s"${tableName}.created_at as audit_created_at",
      s"${tableName}.created_by_guid as audit_created_by_guid"
    ).mkString(",\n    ")
  }

  def queryCreationDefaultingUpdatedAt(tableName: String): String = {
    Seq(
      s"${tableName}.created_at as audit_created_at",
      s"${tableName}.created_by_guid as audit_created_by_guid",
      s"${tableName}.created_at as audit_updated_at",
      s"${tableName}.created_by_guid as audit_updated_by_guid"
    ).mkString(",\n    ")
  }

  def queryWithAlias(tableName: String, prefix: String): String = {
    Seq(
      s"${tableName}.created_at as ${prefix}_audit_created_at",
      s"${tableName}.created_by_guid as ${prefix}_audit_created_by_guid",
      s"${tableName}.updated_at as ${prefix}_audit_updated_at",
      s"${tableName}.updated_by_guid as ${prefix}_audit_updated_by_guid"
    ).mkString(",\n    ")
  }

  def queryCreationWithAlias(tableName: String, prefix: String): String = {
    Seq(
      s"${tableName}.created_at as ${prefix}_audit_created_at",
      s"${tableName}.created_by_guid as ${prefix}_audit_created_by_guid"
    ).mkString(",\n    ")
  }

}
