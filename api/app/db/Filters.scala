package db

object Filters {

  def isDeleted(
    tableName: String,
    value: Boolean
  ): String = {
    value match {
      case true => s"and $tableName.deleted_at is not null"
      case false => s"and $tableName.deleted_at is null"
    }
  }

  def isExpired(
    tableName: String,
    value: Boolean
  ): String = {
    value match {
      case true => { s"and $tableName.expires_at < now()" }
      case false => { s"and $tableName.expires_at >= now()" }
    }
  }

}
